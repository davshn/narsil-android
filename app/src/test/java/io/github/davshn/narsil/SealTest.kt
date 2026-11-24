package io.github.davshn.narsil

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

// The vector was sealed by narsil-claude's lib/seal.js, the same code the mod's hooks/seal.ts mirrors.
class SealTest {
    private val key = "AwoRGB8mLTQ7QklQV15lbHN6gYiPlp2kq7K5wMfO1dw"
    private val inside = "{\"title\":\"Narsil te necesita\",\"message\":\"Una pregunta: ¿sigo con la migración? ✓ — texto largo para cruzar varios bloques de 32 bytes.\"}"
    private val sealed = "narsil1:NNtDuViom-mP8W-HN7f9FIlsPGAAWBMOfT0IinNAtIna3AMHHXow0VA8UxPzNWEyTDSe9gtDEINvu_gdyJzT3EgVH9GC9HcW6MQBYciBIxn0fIBadnuvWvkSGa37CGBRSTod3w1dDUeKG5iPwH9tTTkCq6DOOfYtKgr_o4VklsQ6g_A5Dy8tUel3H883XQDBOrAA2dy3HSyV0b4fA6gtxTi1tkY6HQbhg-KUFfpFFIGP7wkBc9AZ6IJi_coPCg"
    private val otherKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"

    @Test
    fun opensWhatTheModSealed() {
        assertEquals(inside, Seal.open(key, sealed))
    }

    @Test
    fun refusesAnotherKey() {
        try {
            Seal.open(otherKey, sealed)
            fail("another key must not open it")
        } catch (e: IllegalArgumentException) {
            assertEquals("La clave no es la de este aviso", e.message)
        }
    }

    @Test
    fun tellsSealedText() {
        assertTrue(Seal.isSealed(sealed))
        assertFalse(Seal.isSealed("Terminó"))
        assertFalse(Seal.isSealed(null))
    }

    @Test
    fun unsealPutsTitleAndTextBack() {
        val m = Seal.unseal(key, JSONObject().put("id", "a1").put("title", "Narsil").put("message", sealed))
        assertEquals("a1", m.getString("id"))
        assertEquals("Narsil te necesita", m.getString("title"))
        assertTrue(m.getString("message").startsWith("Una pregunta"))
        assertFalse(m.optBoolean("sealed"))
    }

    @Test
    fun unsealLeavesPlainMessagesAlone() {
        val plain = JSONObject().put("title", "Narsil").put("message", "Terminó")
        assertEquals(plain.toString(), Seal.unseal(key, plain).toString())
    }

    @Test
    fun unsealMarksWhatTheKeyCannotOpen() {
        for (k in listOf(otherKey, null, "")) {
            val m = Seal.unseal(k, JSONObject().put("title", "Narsil").put("message", sealed))
            assertTrue(m.getBoolean("sealed"))
            assertEquals("", m.getString("message"))
        }
    }
}
