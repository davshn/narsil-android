package io.github.davshn.narsil

import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionScriptTest {
    private val script = ExtensionScript.build { path -> if (path.endsWith(".css")) "/* $path */ a { content: \"x\" }" else "// $path" }

    @Test
    fun runsTheExtensionScriptsInItsManifestOrder() {
        val order = listOf("host.js", "extension/lib/watch.js", "extension/lib/mode.js", "extension/mode.js", "extension/lib/restore.js", "extension/lib/state.js", "extension/lib/live.js", "extension/content.js")
        val at = order.map { script.indexOf("// $it") }
        assertTrue("every file is in: $at", at.all { it >= 0 })
        assertTrue("in order: $at", at.zipWithNext().all { (a, b) -> a < b })
    }

    @Test
    fun theIdleGroupWaitsForTheDocument() {
        val idle = script.indexOf("const idle = () =>")
        assertTrue(idle in script.indexOf("// extension/mode.js")..script.indexOf("// extension/lib/restore.js"))
        assertTrue(script.indexOf("addEventListener('DOMContentLoaded', idle") > script.indexOf("// extension/content.js"))
    }

    @Test
    fun bothStylesGoInAsOneQuotedString() {
        assertTrue(script.contains("const NARSIL_CSS = \"/* extension/skin.css */ a { content: \\\"x\\\" }\\n/* extension/band.css */"))
    }

    @Test
    fun runsOnlyOnceAndOnlyInTheTopFrame() {
        assertTrue(script.contains("window.top !== window"))
        assertTrue(script.contains("self.narsilHostLoaded"))
    }
}
