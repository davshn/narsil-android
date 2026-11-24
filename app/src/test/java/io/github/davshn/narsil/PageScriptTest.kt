package io.github.davshn.narsil

import org.junit.Assert.assertTrue
import org.junit.Test

class PageScriptTest {
    private val script = PageScript.build { path -> if (path.endsWith(".css")) "/* $path */ a { content: \"x\" }" else "// $path" }

    @Test
    fun runsThePageScriptsInItsManifestOrder() {
        val order = listOf("host.js", "page/lib/watch.js", "page/lib/mode.js", "page/mode.js", "page/lib/restore.js", "page/lib/state.js", "page/lib/live.js", "page/content.js")
        val at = order.map { script.indexOf("// $it") }
        assertTrue("every file is in: $at", at.all { it >= 0 })
        assertTrue("in order: $at", at.zipWithNext().all { (a, b) -> a < b })
    }

    @Test
    fun theIdleGroupWaitsForTheDocument() {
        val idle = script.indexOf("const idle = () =>")
        assertTrue(idle in script.indexOf("// page/mode.js")..script.indexOf("// page/lib/restore.js"))
        assertTrue(script.indexOf("addEventListener('DOMContentLoaded', idle") > script.indexOf("// page/content.js"))
    }

    @Test
    fun bothStylesGoInAsOneQuotedString() {
        assertTrue(script.contains("const NARSIL_CSS = \"/* page/skin.css */ a { content: \\\"x\\\" }\\n/* page/band.css */"))
    }

    @Test
    fun runsOnlyOnceAndOnlyInTheTopFrame() {
        assertTrue(script.contains("window.top !== window"))
        assertTrue(script.contains("self.narsilHostLoaded"))
    }
}
