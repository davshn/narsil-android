package io.github.davshn.narsil

import org.json.JSONObject

// What a notification shows of an ntfy.sh message: a question or an error
// (priority 4 and up) stays and buzzes its own way; a "done" takes the place of the last one of its conversation (its
// `conv-` tag); ntfy's "view" actions as buttons, two at most; and a tap opens Claude Code in the app.
data class Alert(
    val title: String,
    val body: String,
    val isUrgent: Boolean,
    val tag: String,
    val views: List<View>,
    val icon: String?,
    val opens: String,
) {
    data class View(val label: String, val url: String)

    companion object {
        // Without a key anyone who knows the topic can publish on it: a picture only comes from Narsil's own assets.
        private const val ART = "https://raw.githubusercontent.com/davshn/narsil-pet/main/assets/"
        private const val SEALED = "🔒 Aviso cifrado: pon la clave en los ajustes de la app."

        fun of(m: JSONObject): Alert {
            val isUrgent = m.optInt("priority", 3) >= 4
            val tags = m.optJSONArray("tags")
            val conversation = (0 until (tags?.length() ?: 0)).map { tags!!.optString(it) }.firstOrNull { it.startsWith("conv-") }
            val id = m.optString("id").ifEmpty { m.optLong("time").toString() }
            val actions = m.optJSONArray("actions")
            val views = (0 until (actions?.length() ?: 0)).mapNotNull { actions!!.optJSONObject(it) }
                .filter { it.optString("action") == "view" && it.optString("url").isNotEmpty() }
                .take(2)
                .map { View(it.optString("label").ifEmpty { "Abrir" }, it.optString("url")) }
            val click = m.optString("click").takeIf { Links.isClaude(it) }
            return Alert(
                title = m.optString("title").ifEmpty { "Narsil" },
                body = if (m.optBoolean("sealed")) SEALED else m.optString("message"),
                isUrgent = isUrgent,
                tag = if (!isUrgent && conversation != null) "done-$conversation" else id,
                views = views,
                icon = m.optString("icon").takeIf { it.startsWith(ART) },
                opens = click ?: Links.CLAUDE_CODE,
            )
        }
    }
}
