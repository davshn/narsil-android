package io.github.davshn.narsil

import org.json.JSONObject

// The band's state, followed from <topic>-estado while a page shows the band: what ntfy.sh kept of the last 12 h
// first, then what comes. Each change goes to the band as a message on its port: {type: 'model', model} or
// {type: 'no-topic'}. `post` is called on the stream's thread.
class StateFeed(private val settings: Settings, private val post: (String) -> Unit) {
    private var stream: NtfyStream? = null
    private var model = StateModel.empty()
    private var run = 0

    @Synchronized
    fun start() {
        stop()
        val mine = run
        val topic = settings.topic
        if (topic == null) {
            post(JSONObject().put("type", "no-topic").toString())
            return
        }
        val key = settings.key
        model = settings.model(topic) ?: StateModel.empty()
        send()
        stream = NtfyStream(NtfyStream.topicUrl("$topic-estado"), "12h") { raw ->
            synchronized(this) {
                if (mine != run) return@synchronized
                model = StateModel.receive(model, raw, key)
                settings.saveModel(topic, model)
                send()
            }
        }.also { it.start() }
    }

    @Synchronized
    fun stop() {
        run += 1
        stream?.stop()
        stream = null
    }

    private fun send() = post(JSONObject().put("type", "model").put("model", model).toString())
}
