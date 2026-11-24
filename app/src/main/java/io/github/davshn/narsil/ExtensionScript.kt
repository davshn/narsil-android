package io.github.davshn.narsil

// The one script the WebView runs at document start on claude.ai: host.js, which answers what narsil-claude's content
// scripts ask of their browser, with the extension's styles, then its scripts in its manifest's order: the
// document_start group at once and the document_idle group once the document is parsed. Each file runs in a function
// of its own, so one that throws leaves the others running. `read` gives an asset's text.
object ExtensionScript {
    private val STYLES = listOf("extension/skin.css", "extension/band.css")
    private val AT_START = listOf("extension/lib/watch.js", "extension/lib/mode.js", "extension/mode.js")
    private val AT_IDLE = listOf("extension/lib/restore.js", "extension/lib/state.js", "extension/lib/live.js", "extension/content.js")

    fun build(read: (String) -> String): String {
        val css = STYLES.joinToString("\n") { read(it) }
        fun run(path: String) = "run(${jsString(path)}, function () {\n${read(path)}\n})\n"
        return buildString {
            append("(() => {\n")
            append("if (window.top !== window || self.narsilHostLoaded || !self.narsilHost) return\n")
            append("self.narsilHostLoaded = true\n")
            append("const run = (name, code) => { try { code() } catch (error) { console.error('narsil: ' + name, error) } }\n")
            append("const NARSIL_CSS = ${jsString(css)}\n")
            append(read("host.js")).append('\n')
            AT_START.forEach { append(run(it)) }
            append("self.narsilWatch(adopt)\n")
            append("const idle = () => {\n")
            AT_IDLE.forEach { append(run(it)) }
            append("}\n")
            append("if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', idle, { once: true })\n")
            append("else idle()\n")
            append("})()\n")
        }
    }

    // A JavaScript string literal holding `text` as it is.
    fun jsString(text: String): String = buildString {
        append('"')
        for (c in text) {
            when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                ' ' -> append("\\u2028")
                ' ' -> append("\\u2029")
                else -> append(c)
            }
        }
        append('"')
    }
}
