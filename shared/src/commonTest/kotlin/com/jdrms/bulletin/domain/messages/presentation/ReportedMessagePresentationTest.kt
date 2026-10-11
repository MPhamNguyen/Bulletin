package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.conversation
import com.jdrms.bulletin.domain.messages.message
import kotlin.test.Test
import kotlin.test.assertEquals

class ReportedMessagePresentationTest {
    @Test
    fun reportedContentIsHiddenInThreadAndPreviewUntilExplicitlyRevealed() {
        val reported = message().copy(content = "Sensitive content", isReported = true)
        val visible = message().copy(content = "Ordinary content")
        assertEquals("Reported message hidden", messageBodyText(reported, false))
        assertEquals("Sensitive content", messageBodyText(reported, true))
        assertEquals("Ordinary content", messageBodyText(visible, false))
        assertEquals(
            "Reported message hidden",
            conversationPreviewText(conversation().recordMessage(reported).getOrThrow())
        )
        assertEquals("Ordinary content", conversationPreviewText(conversation().recordMessage(visible).getOrThrow()))
        assertEquals("No messages yet", conversationPreviewText(conversation()))
    }
}
