package io.github.davshn.narsil

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

// The topic /narsil movil gives, and the key of /narsil movil clave when there is one: kept on the phone only.
class SettingsActivity : ComponentActivity() {
    private lateinit var settings: Settings
    private lateinit var status: TextView
    private lateinit var battery: Button
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) status.text = getString(R.string.settings_no_notifications)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        settings = Settings(this)
        val topic = findViewById<EditText>(R.id.topic)
        val key = findViewById<EditText>(R.id.key)
        status = findViewById(R.id.status)
        battery = findViewById(R.id.battery)
        topic.setText(settings.topic.orEmpty())
        key.setText(settings.key.orEmpty())

        findViewById<Button>(R.id.save).setOnClickListener {
            val wanted = topic.text.toString().trim()
            if (!Settings.isTopic(wanted)) {
                status.text = getString(R.string.settings_bad_topic)
                return@setOnClickListener
            }
            if (wanted != settings.topic) settings.topic = wanted
            settings.key = key.text.toString().trim()
            AlertService.start(this)
            status.text = getString(R.string.settings_saved)
            askForNotifications()
        }
        battery.setOnClickListener { askToRunInBackground() }
    }

    override fun onResume() {
        super.onResume()
        battery.isEnabled = !runsInBackground()
        battery.text = getString(if (battery.isEnabled) R.string.settings_battery else R.string.settings_battery_done)
    }

    private fun runsInBackground() = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

    // Without it, Android cuts the connection while the phone sleeps and alerts come late.
    @SuppressLint("BatteryLife")
    private fun askToRunInBackground() {
        startActivity(Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
    }

    private fun askForNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
