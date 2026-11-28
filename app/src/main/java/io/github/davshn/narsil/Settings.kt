package io.github.davshn.narsil

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

// What the app keeps on the phone: the topic and the key typed in its settings (never in its code, as the repository
// is public), the last alert it showed, so a restart goes on from it, and the band's last state, per topic.
class Settings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("narsil", Context.MODE_PRIVATE)

    var topic: String?
        get() = prefs.getString(TOPIC, null)?.takeIf { isTopic(it) }
        set(value) = prefs.edit { putString(TOPIC, value).remove(LAST_ALERT) }

    var key: String?
        get() = prefs.getString(KEY, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit { putString(KEY, value) }

    var lastAlertId: String?
        get() = prefs.getString(LAST_ALERT, null)
        set(value) = prefs.edit { putString(LAST_ALERT, value) }

    fun model(topic: String): JSONObject? = try {
        prefs.getString(MODEL, null)?.let(::JSONObject)?.takeIf { it.optString("topic") == topic }?.optJSONObject("model")
    } catch (_: Exception) {
        null
    }

    fun saveModel(topic: String, model: JSONObject) {
        prefs.edit { putString(MODEL, JSONObject().put("topic", topic).put("model", model).toString()) }
    }

    companion object {
        private const val TOPIC = "topic"
        private const val KEY = "key"
        private const val LAST_ALERT = "lastAlertId"
        private const val MODEL = "band"
        private val TOPIC_SHAPE = Regex("^[-_A-Za-z0-9]{1,64}$")

        fun isTopic(topic: String?): Boolean = topic != null && TOPIC_SHAPE.matches(topic)
    }
}
