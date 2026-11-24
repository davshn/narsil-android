package io.github.davshn.narsil

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// How Narsil's ntfy.sh alerts show as notifications.
class AlertTest {
    private fun m() = JSONObject().put("id", "m1").put("title", "Narsil").put("message", "Terminó")

    @Test
    fun aQuestionOrAnErrorStaysAndBuzzes() {
        val alert = Alert.of(m().put("priority", 4).put("tags", JSONArray().put("conv-abc")))
        assertTrue(alert.isUrgent)
        assertEquals("m1", alert.tag)
    }

    @Test
    fun aDoneReplacesTheLastOfItsConversation() {
        val alert = Alert.of(m().put("tags", JSONArray().put("white_check_mark").put("conv-abc")))
        assertFalse(alert.isUrgent)
        assertEquals("done-conv-abc", alert.tag)
    }

    @Test
    fun aDoneWithoutConversationKeepsItsOwnPlace() {
        assertEquals("m1", Alert.of(m()).tag)
    }

    @Test
    fun aSealedAlertAsksForTheKey() {
        val alert = Alert.of(m().put("message", "").put("sealed", true))
        assertEquals("🔒 Aviso cifrado: pon la clave en los ajustes de la app.", alert.body)
    }

    @Test
    fun noTitleIsNarsil() {
        assertEquals("Narsil", Alert.of(JSONObject().put("id", "m2").put("message", "hola")).title)
    }

    @Test
    fun viewActionsBecomeTwoButtonsAtMost() {
        val actions = JSONArray()
            .put(JSONObject().put("action", "view").put("label", "Abrir").put("url", "https://claude.ai/code/session_1"))
            .put(JSONObject().put("action", "http").put("label", "No").put("url", "https://example.com"))
            .put(JSONObject().put("action", "view").put("label", "PR").put("url", "https://github.com/x"))
            .put(JSONObject().put("action", "view").put("label", "Tercera").put("url", "https://github.com/y"))
        val views = Alert.of(m().put("actions", actions)).views
        assertEquals(listOf("Abrir", "PR"), views.map { it.label })
    }

    @Test
    fun aTapOpensClaudeCodeOrTheClaudeAddressItCarries() {
        assertEquals("https://claude.ai/code", Alert.of(m()).opens)
        assertEquals("https://claude.ai/code/session_9", Alert.of(m().put("click", "https://claude.ai/code/session_9")).opens)
        assertEquals("https://claude.ai/code", Alert.of(m().put("click", "https://evil.example/claude.ai")).opens)
    }

    @Test
    fun onlyNarsilsOwnArtIsShown() {
        val art = "https://raw.githubusercontent.com/davshn/narsil-pet/main/assets/necesita-oscuro.png"
        assertEquals(art, Alert.of(m().put("icon", art)).icon)
        assertNull(Alert.of(m().put("icon", "https://example.com/a.png")).icon)
    }
}
