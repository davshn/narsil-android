package io.github.davshn.narsil

import org.json.JSONException
import org.json.JSONObject

// What the band keeps of the states the mod sends to <topic>-estado, as the band's lib/state.js receive does:
// the newest state, opened first when sealed, or a mark that the key could not open it.
object StateModel {
    fun empty(): JSONObject = JSONObject().put("state", JSONObject.NULL).put("isSealed", false)

    fun receive(model: JSONObject, raw: JSONObject, key: String?): JSONObject {
        if (raw.optString("event") != "message") return model
        val m = Seal.unseal(key, raw)
        if (m.optBoolean("sealed")) return JSONObject(model.toString()).put("isSealed", true)
        val state = try {
            JSONObject(m.optString("message"))
        } catch (_: JSONException) {
            return model
        }
        val kept = model.optJSONObject("state")?.optDouble("at", 0.0) ?: 0.0
        if (kept > state.optDouble("at", 0.0)) return model
        return JSONObject().put("state", state).put("isSealed", false)
    }
}
