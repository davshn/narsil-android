package io.github.davshn.narsil

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

// Opens what the mod sealed with /narsil movil clave (hooks/seal.ts in narsil-pet), byte for byte as it seals it:
// "narsil1:" and then, in base64url, a 16-byte nonce, the ciphertext and a 32-byte tag. HMAC-SHA256 of the nonce
// and a block counter is the keystream; HMAC-SHA256 over the nonce and the ciphertext the tag; both keys come from the
// shared one, as HMAC of "enc" and "mac".
object Seal {
    private const val PREFIX = "narsil1:"
    private const val NONCE = 16
    private const val TAG = 32

    fun isSealed(text: String?): Boolean = text?.startsWith(PREFIX) == true

    // The text inside, or IllegalArgumentException when the key is not the one that sealed it.
    fun open(key: String, text: String): String {
        val bytes = bytesOf(text.removePrefix(PREFIX))
        val shared = bytesOf(key)
        require(bytes.size >= NONCE + TAG && shared.isNotEmpty()) { WRONG_KEY }
        val encKey = hmac(shared, "enc".toByteArray())
        val macKey = hmac(shared, "mac".toByteArray())
        val body = bytes.copyOfRange(0, bytes.size - TAG)
        val given = bytes.copyOfRange(bytes.size - TAG, bytes.size)
        require(MessageDigest.isEqual(hmac(macKey, body), given)) { WRONG_KEY }
        val nonce = body.copyOfRange(0, NONCE)
        val cipher = body.copyOfRange(NONCE, body.size)
        val plain = ByteArray(cipher.size)
        var block = 0
        while (block * 32 < cipher.size) {
            val stream = hmac(encKey, ByteBuffer.allocate(NONCE + 4).put(nonce).putInt(block).array())
            for (j in 0 until 32) {
                val i = block * 32 + j
                if (i >= cipher.size) break
                plain[i] = (cipher[i].toInt() xor stream[j].toInt()).toByte()
            }
            block++
        }
        return String(plain, Charsets.UTF_8)
    }

    // An ntfy message with its title and text back in place: unchanged when not sealed; with `sealed: true` and no
    // text when the key cannot open it.
    fun unseal(key: String?, m: JSONObject): JSONObject {
        if (!isSealed(m.optString("message"))) return m
        val out = JSONObject(m.toString())
        try {
            val inside = JSONObject(open(key.orEmpty(), m.getString("message")))
            if (inside.has("title")) out.put("title", inside.get("title"))
            out.put("message", inside.optString("message"))
        } catch (_: Exception) {
            out.put("message", "").put("sealed", true)
        }
        return out
    }

    private const val WRONG_KEY = "La clave no es la de este aviso"

    // As the page's atob after its -/_ swap: standard or url-safe base64, with or without padding.
    private fun bytesOf(text: String): ByteArray = try {
        Base64.getUrlDecoder().decode(text.trim().replace('+', '-').replace('/', '_').trimEnd('='))
    } catch (_: IllegalArgumentException) {
        ByteArray(0)
    }

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(key, "HmacSHA256"))
            doFinal(data)
        }
}
