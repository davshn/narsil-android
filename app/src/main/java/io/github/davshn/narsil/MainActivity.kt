package io.github.davshn.narsil

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.util.Calendar
import org.json.JSONObject

// claude.ai in a WebView, dressed by narsil-claude: its content scripts run here as they run in Vivaldi, at document
// start, and the app answers what they ask of their browser (host.js). The extension's packaged files are served from
// https://claude.ai/__narsil__/, where the page's own policy lets them load.
class MainActivity : ComponentActivity() {
    private lateinit var web: WebView
    private lateinit var settings: Settings
    private lateinit var feed: StateFeed
    private lateinit var frame: FrameLayout

    @Volatile
    private var reply: JavaScriptReplyProxy? = null
    private var chosen: ValueCallback<Array<Uri>>? = null
    private var microphone: PermissionRequest? = null

    private val pickFiles = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        chosen?.onReceiveValue(uris.toTypedArray())
        chosen = null
    }
    private val askMicrophone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val request = microphone ?: return@registerForActivityResult
        microphone = null
        if (granted) request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) else request.deny()
    }
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        settings = Settings(this)
        // A debug build's page can be inspected from a computer, at chrome://inspect.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) WebView.setWebContentsDebuggingEnabled(true)
        feed = StateFeed(settings) { text -> runOnUiThread { reply?.postMessage(text) } }

        web = WebView(this)
        frame = FrameLayout(this).apply { addView(web) }
        setContentView(frame)
        // The page goes between the system bars and above the keyboard, which Android 15 draws over the app.
        ViewCompat.setOnApplyWindowInsetsListener(frame) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            // A page's popups (Google's sign-in button opens one) reach onCreateWindow, which decides where they go.
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(web, true)
        }
        host()
        web.webViewClient = Client()
        web.webChromeClient = Chrome()

        onBackPressedDispatcher.addCallback(this) {
            if (web.canGoBack()) web.goBack() else moveTaskToBack(true)
        }

        web.loadUrl(claudeIn(intent) ?: Links.CLAUDE_CODE)
        Notifier.createChannels(this)
        if (settings.topic != null) {
            askForNotifications()
            AlertService.start(this)
        }
    }

    // The extension's scripts and the channel they talk to the app through, on claude.ai only.
    private fun host() {
        val origins = setOf(ORIGIN)
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) ||
            !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        ) {
            Log.w(TAG, "This WebView cannot host the extension: update Android System WebView")
            return
        }
        WebViewCompat.addWebMessageListener(web, "narsilHost", origins) { _, message, origin, isMainFrame, proxy ->
            if (!isMainFrame || origin.toString() != ORIGIN) return@addWebMessageListener
            val m = try {
                JSONObject(message.data ?: return@addWebMessageListener)
            } catch (_: Exception) {
                return@addWebMessageListener
            }
            when (m.optString("kind")) {
                "connect" -> {
                    reply = proxy
                    feed.start()
                }
                "disconnect" -> if (reply === proxy) {
                    reply = null
                    feed.stop()
                }
                "port" -> if (m.optJSONObject("message")?.optString("type") == "settings") openSettings()
                "settings" -> openSettings()
            }
        }
        val script = ExtensionScript.build { path -> assets.open(path).bufferedReader().use { it.readText() } }
        WebViewCompat.addDocumentStartJavaScript(web, script, origins)
    }

    private inner class Client : WebViewClient() {
        private val files = WebViewAssetLoader.Builder()
            .setDomain("claude.ai")
            .addPathHandler("/__narsil__/", WebViewAssetLoader.AssetsPathHandler(this@MainActivity))
            .build()

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
            files.shouldInterceptRequest(request.url)

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
            !Links.isInApp(request.url.toString()).also { inApp -> if (!inApp) goOut(request.url) }

        override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
            reply = null
            feed.stop()
        }
    }

    // Where an address the page leaves claude.ai for goes: Google's and Apple's sign-ins get a word on using an email,
    // as they refuse to run here; anything else opens in the phone's browser.
    private fun goOut(url: Uri) {
        if (Links.isOutsideSignIn(url.toString())) {
            Toast.makeText(this, R.string.sign_in_with_email, Toast.LENGTH_LONG).show()
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url).addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (_: ActivityNotFoundException) {
            Log.w(TAG, "Nothing opens $url")
        }
    }

    private inner class Chrome : WebChromeClient() {
        // A popup never shows: a WebView of its own only hears the address it wants, which then goes where any link
        // the page follows goes. The popup's opener sees it closed.
        override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message): Boolean {
            val popup = WebView(this@MainActivity)
            popup.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(popupView: WebView, request: WebResourceRequest): Boolean {
                    val url = request.url
                    if (Links.isInApp(url.toString())) web.loadUrl(url.toString()) else goOut(url)
                    popupView.post(popupView::destroy)
                    return true
                }
            }
            (resultMsg.obj as WebView.WebViewTransport).webView = popup
            resultMsg.sendToTarget()
            return true
        }

        override fun onConsoleMessage(message: ConsoleMessage): Boolean {
            Log.d(WEB_TAG, "${message.messageLevel()} ${message.message()} (${message.sourceId()}:${message.lineNumber()})")
            return true
        }

        override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
            chosen?.onReceiveValue(null)
            chosen = callback
            val types = params.acceptTypes.filter { it.isNotBlank() }.ifEmpty { listOf("*/*") }
            try {
                pickFiles.launch(types.toTypedArray())
            } catch (_: ActivityNotFoundException) {
                chosen = null
                return false
            }
            return true
        }

        // The microphone, for dictating to Claude; nothing else the page may ask for.
        override fun onPermissionRequest(request: PermissionRequest) {
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE !in request.resources || !Links.isClaude(request.origin.toString())) {
                request.deny()
                return
            }
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
            } else {
                microphone = request
                askMicrophone.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        claudeIn(intent)?.let(web::loadUrl)
    }

    // A claude.ai address the app was opened with: from an alert, or a link Android hands it.
    private fun claudeIn(intent: Intent?): String? = intent?.data?.toString()?.takeIf(Links::isClaude)

    override fun onResume() {
        super.onResume()
        web.onResume()
        paintBars()
    }

    // The session's cookie reaches the disk now, not when Android gets to it: a killed app keeps its sign-in.
    override fun onPause() {
        CookieManager.getInstance().flush()
        web.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        feed.stop()
        web.destroy()
        super.onDestroy()
    }

    // Behind the system bars, the page's own ground, light by day and dark by night as narsil-claude's mode.js sets it.
    private fun paintBars() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isDay = hour in 6..17
        frame.setBackgroundColor(Color.parseColor(if (isDay) "#c9e7f1" else "#0f1a2c"))
        WindowCompat.getInsetsController(window, frame).apply {
            isAppearanceLightStatusBars = isDay
            isAppearanceLightNavigationBars = isDay
        }
    }

    private fun askForNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openSettings() = runOnUiThread { startActivity(Intent(this, SettingsActivity::class.java)) }

    companion object {
        private const val ORIGIN = "https://claude.ai"
        private const val TAG = "Narsil"
        private const val WEB_TAG = "NarsilWeb"
    }
}
