package io.github.davshn.narsil

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat

// Follows the alerts topic while the phone is on: a foreground service of the "specialUse" type, as Android 15 caps
// "dataSync" ones at six hours a day. It goes on from the last alert it showed,
// so one sent while the phone was off or the service was stopped comes once it is back, as ntfy.sh keeps them 12 h.
class AlertService : Service() {
    private var stream: NtfyStream? = null
    private var topic: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val settings = Settings(this)
        val wanted = settings.topic
        if (wanted == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        Notifier.createChannels(this)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, Notifier.SERVICE_ID, Notifier.serviceNotification(this, wanted), type)
        if (wanted != topic || stream == null) {
            stream?.stop()
            topic = wanted
            stream = NtfyStream(NtfyStream.topicUrl(wanted), settings.lastAlertId, ::take).also { it.start() }
        }
        return START_STICKY
    }

    private fun take(raw: org.json.JSONObject) {
        val settings = Settings(this)
        Notifier.show(this, Alert.of(Seal.unseal(settings.key, raw)))
        raw.optString("id").takeIf { it.isNotEmpty() }?.let { settings.lastAlertId = it }
    }

    override fun onDestroy() {
        stream?.stop()
        stream = null
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            if (Settings(context).topic != null) context.startForegroundService(Intent(context, AlertService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlertService::class.java))
        }
    }
}
