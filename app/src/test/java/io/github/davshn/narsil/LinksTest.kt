package io.github.davshn.narsil

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinksTest {
    @Test
    fun claudeStaysInTheApp() {
        assertTrue(Links.isClaude("https://claude.ai/code"))
        assertTrue(Links.isInApp("https://claude.ai/login"))
        assertTrue(Links.isInApp("https://challenges.cloudflare.com/turnstile"))
    }

    @Test
    fun everythingElseGoesOut() {
        assertFalse(Links.isClaude("http://claude.ai/code"))
        assertFalse(Links.isClaude("https://claude.ai.evil.example/"))
        assertFalse(Links.isInApp("https://accounts.google.com/o/oauth2"))
        assertFalse(Links.isInApp("https://github.com/davshn"))
        assertFalse(Links.isInApp("not a url"))
    }
}
