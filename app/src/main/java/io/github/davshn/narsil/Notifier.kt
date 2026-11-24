package io.github.davshn.narsil

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.net.HttpURLConnection
import java.net.URL

// Narsil's notifications: what needs you on a channel that buzzes its own way, a "done" on a quieter one, and the
// service's own, which Android asks of every app that stays connected.
object Notifier {
    private const val NEEDS = "needs"
    private const val DONE = "done"
    private const val SERVICE = "service"
    const val SERVICE_ID = 1
    private val NEEDS_BUZZ = longArrayOf(0, 300, 120, 300, 120, 600)
    private val DONE_BUZZ = longArrayOf(0, 200)

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(NEEDS, "Te necesita", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Una pregunta o un error de Claude"
                    enableVibration(true)
                    vibrationPattern = NEEDS_BUZZ
                },
                NotificationChannel(DONE, "Terminó", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Claude terminó un turno"
                    enableVibration(true)
                    vibrationPattern = DONE_BUZZ
                },
                NotificationChannel(SERVICE, "Conexión", NotificationManager.IMPORTANCE_MIN).apply {
                    description = "La conexión con ntfy.sh que trae los avisos"
                    setShowBadge(false)
                },
            ),
        )
    }

    fun serviceNotification(context: Context, topic: String): Notification =
        NotificationCompat.Builder(context, SERVICE)
            .setSmallIcon(R.drawable.ic_stat_narsil)
            .setContentTitle("Narsil escucha tus avisos")
            .setContentText("Topic $topic · toca para cambiarlo")
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .build()

    // Posting is checked: the person may have turned notifications off.
    @SuppressLint("MissingPermission")
    fun show(context: Context, alert: Alert) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val code = alert.tag.hashCode()
        val builder = NotificationCompat.Builder(context, if (alert.isUrgent) NEEDS else DONE)
            .setSmallIcon(R.drawable.ic_stat_narsil)
            .setContentTitle(alert.title)
            .setContentText(alert.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.body))
            .setPriority(if (alert.isUrgent) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(if (alert.isUrgent) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(open(context, alert.opens, code))
        picture(alert.icon)?.let(builder::setLargeIcon)
        alert.views.forEachIndexed { i, view -> builder.addAction(0, view.label, open(context, view.url, code + i + 1)) }
        manager.notify(alert.tag, 0, builder.build())
    }

    // claude.ai opens in the app; any other address in the phone's browser.
    private fun open(context: Context, url: String, code: Int): PendingIntent {
        val intent = if (Links.isClaude(url)) {
            Intent(Intent.ACTION_VIEW, Uri.parse(url), context, MainActivity::class.java)
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    // Narsil's art, from her own assets only (Alert.of checks the address); none when it does not come in time.
    private fun picture(url: String?): Bitmap? {
        if (url == null) return null
        return try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 5_000
            c.readTimeout = 5_000
            try {
                c.inputStream.use(BitmapFactory::decodeStream)
            } finally {
                c.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }
}
