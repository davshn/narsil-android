package io.github.davshn.narsil

// The one script the WebView runs at document start on claude.ai: host.js, which answers what the band's scripts ask of
// their browser, with Narsil's styles, then the scripts in their order: Narsil's colors and the light or dark mode at
// once, and the band once the document is parsed. Each file runs in a function of its own, so one that throws leaves
// the others running. `read` gives an asset's text.
object PageScript {
    private val STYLES = listOf("page/skin.css", "page/band.css")
    private val AT_START = listOf("page/lib/watch.js", "page/lib/mode.js", "page/mode.js")
    private val AT_IDLE = listOf("page/lib/restore.js", "page/lib/state.js", "page/lib/live.js", "page/content.js")

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
