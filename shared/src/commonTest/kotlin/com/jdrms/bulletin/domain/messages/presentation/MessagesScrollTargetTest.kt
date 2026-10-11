package com.jdrms.bulletin.domain.messages.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MessagesScrollTargetTest {
    @Test
    fun emptyMessagesListHasNoScrollTarget() {
        assertNull(messageScrollTargetIndex(messagesCount = 0, hasOlderCursor = false))
        assertNull(messageScrollTargetIndex(messagesCount = 0, hasOlderCursor = true))
    }

    @Test
    fun scrollTargetIndexAccountsForOlderHistoryButton() {
        assertEquals(0, messageScrollTargetIndex(messagesCount = 1, hasOlderCursor = false))
        assertEquals(1, messageScrollTargetIndex(messagesCount = 1, hasOlderCursor = true))
        assertEquals(4, messageScrollTargetIndex(messagesCount = 5, hasOlderCursor = false))
        assertEquals(5, messageScrollTargetIndex(messagesCount = 5, hasOlderCursor = true))
    }

    @Test
    fun newAppendedMessageAdvancesScrollTargetIndex() {
        val initialIndex = messageScrollTargetIndex(messagesCount = 3, hasOlderCursor = false)
        val afterNewMessageIndex = messageScrollTargetIndex(messagesCount = 4, hasOlderCursor = false)

        assertEquals(2, initialIndex)
        assertEquals(3, afterNewMessageIndex)
    }
}
