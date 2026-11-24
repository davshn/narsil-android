package io.github.davshn.narsil

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// As narsil-claude's lib/state.js receive: what the band keeps of the states the mod sends to <topic>-estado.
class StateModelTest {
    private fun message(text: String) = JSONObject().put("event", "message").put("id", "x").put("message", text)

    @Test
    fun keepsTheState() {
        val model = StateModel.receive(StateModel.empty(), message("{\"at\":10,\"xp\":\"⭐ Nivel 2 · 5/10 XP\"}"), null)
        assertEquals(10, model.getJSONObject("state").getInt("at"))
        assertFalse(model.getBoolean("isSealed"))
    }

    @Test
    fun keepsTheNewerOfTwo() {
        val newer = StateModel.receive(StateModel.empty(), message("{\"at\":20}"), null)
        assertEquals(20, StateModel.receive(newer, message("{\"at\":10}"), null).getJSONObject("state").getInt("at"))
        assertEquals(30, StateModel.receive(newer, message("{\"at\":30}"), null).getJSONObject("state").getInt("at"))
    }

    @Test
    fun ignoresWhatIsNotAState() {
        val model = StateModel.empty()
        assertEquals(model.toString(), StateModel.receive(model, JSONObject().put("event", "keepalive"), null).toString())
        assertEquals(model.toString(), StateModel.receive(model, message("not json"), null).toString())
        assertEquals(model.toString(), StateModel.receive(model, message("7"), null).toString())
    }

    @Test
    fun marksASealedStateItCannotOpen() {
        val sealed = message("narsil1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
        val model = StateModel.receive(StateModel.empty(), sealed, "AAAA")
        assertTrue(model.getBoolean("isSealed"))
        assertTrue(model.isNull("state"))
    }
}
