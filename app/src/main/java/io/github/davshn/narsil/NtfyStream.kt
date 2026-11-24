package io.github.davshn.narsil

import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject

// One of ntfy.sh's topics as its JSON stream, a message a line, on a thread of its own, as the band's
// lib/stream.js follows it: when the stream ends it is followed again from the last message it brought; when it fails,
// a little later each time, up to a minute. ntfy.sh sends a keepalive every 45 s, so a read that waits 90 s found a
// dead connection. `since`: a message id or a duration such as "12h"; null for what comes from now on.
class NtfyStream(
    private val topicUrl: String,
    since: String?,
    private val onMessage: (JSONObject) -> Unit,
) {
    @Volatile
    var since: String? = since
        private set

    @Volatile
    private var isRunning = false

    @Volatile
    private var connection: HttpURLConnection? = null
    private var thread: Thread? = null

    fun start() {
        if (isRunning) return
        isRunning = true
        thread = Thread(::follow, "ntfy-stream").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        isRunning = false
        connection?.disconnect()
        thread?.interrupt()
        thread = null
    }

    private fun follow() {
        var delay = FIRST_WAIT
        while (isRunning) {
            try {
                read()
                delay = FIRST_WAIT
                Thread.sleep(FIRST_WAIT)
            } catch (_: InterruptedException) {
                return
            } catch (e: Exception) {
                if (!isRunning) return
                Log.w(TAG, "ntfy.sh stream failed, again in ${delay / 1000} s: ${e.message}")
                try {
                    Thread.sleep(delay)
                } catch (_: InterruptedException) {
                    return
                }
                delay = (delay * 2).coerceAtMost(LAST_WAIT)
            }
        }
    }

    private fun read() {
        val from = since
        val url = URL(if (from != null) "$topicUrl?since=${URLEncoder.encode(from, "UTF-8")}" else topicUrl)
        val c = url.openConnection() as HttpURLConnection
        connection = c
        try {
            c.connectTimeout = 15_000
            c.readTimeout = 90_000
            c.useCaches = false
            if (c.responseCode != 200) throw IOException("ntfy.sh answered ${c.responseCode}")
            c.inputStream.bufferedReader().useLines { lines ->
                for (line in lines) {
                    if (!isRunning) return
                    val raw = try {
                        JSONObject(line)
                    } catch (_: Exception) {
                        continue
                    }
                    if (raw.optString("event") != "message") continue
                    try {
                        onMessage(raw)
                    } catch (e: Exception) {
                        Log.w(TAG, "a message could not be taken: ${e.message}")
                    }
                    raw.optString("id").takeIf { it.isNotEmpty() }?.let { since = it }
                }
            }
        } finally {
            c.disconnect()
            connection = null
        }
    }

    companion object {
        private const val TAG = "NarsilNtfy"
        private const val FIRST_WAIT = 1_000L
        private const val LAST_WAIT = 60_000L

        fun topicUrl(topic: String) = "https://ntfy.sh/$topic/json"
    }
}
