package com.jdrms.bulletin.domain.messages.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class InAppMessageBannerTextTest {
    @Test
    fun `blank sender name falls back`() {
        assertEquals("New message", bannerSenderName("   "))
        assertEquals("Sam", bannerSenderName(" Sam "))
    }

    @Test
    fun `preview collapses whitespace and line breaks`() {
        assertEquals("is this still available?", bannerPreview("  is this  still\n\navailable? "))
    }
}
