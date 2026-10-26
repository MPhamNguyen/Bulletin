package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.EditMessage
import com.jdrms.bulletin.domain.messages.domain.model.MessageEdit
import com.jdrms.bulletin.domain.messages.domain.model.MessageEditException
import com.jdrms.bulletin.domain.messages.domain.model.MessageEditFailure
import com.jdrms.bulletin.domain.messages.domain.model.MessageEditing
import com.jdrms.bulletin.domain.messages.domain.service.MessageTimeline
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class MessageEditingTest {
    private val original = message()
    private val now = original.orderingTimestamp + 1.minutes
    private val edit = MessageEdit(conversationId, original.id, "  Updated  ", null)

    @Test
    fun editingPreservesIdentityAndOrderingAndTrimsContent() {
        val saved = MessageEditing.apply(original, alice.id, edit, now).getOrThrow()
        assertEquals(original.copy(content = "Updated", editedAt = now), saved)
        assertEquals(original.orderingTimestamp, saved.orderingTimestamp)
    }

    @Test
    fun originalCreationDeterminesTheStrictWindowEvenAfterAnEdit() {
        val deadline = original.orderingTimestamp + 15.minutes
        assertTrue(MessageEditing.canEdit(original, alice.id, deadline - 1.minutes))
        assertFalse(MessageEditing.canEdit(original, alice.id, deadline))
        assertFailure(MessageEditFailure.EXPIRED, original, edit, deadline)
        val previouslyEdited = original.copy(editedAt = now)
        assertFailure(MessageEditFailure.EXPIRED, previouslyEdited, edit.copy(expectedEditedAt = now), deadline)
    }

    @Test
    fun ownershipDeletionReportsAndExpectedVersionAreEnforced() {
        assertFalse(MessageEditing.canEdit(original, carol.id, now))
        assertFailure(MessageEditFailure.UNAVAILABLE, original.copy(senderId = carol.id))
        assertFailure(MessageEditFailure.UNAVAILABLE, original.copy(deletedAt = now))
        assertFailure(MessageEditFailure.REPORTED, original.report())
        assertFailure(MessageEditFailure.CONFLICT, original.copy(editedAt = now))
    }

    @Test
    fun unchangedContentIsANoopAndContentBoundariesAreChecked() {
        assertEquals(
            original,
            MessageEditing.apply(original, alice.id, edit.copy(content = " Hello "), now).getOrThrow()
        )
        listOf(" ", "a".repeat(1001), "😀".repeat(501)).forEach {
            assertFailure(MessageEditFailure.INVALID, original, edit.copy(content = it))
        }
        assertTrue(MessageEditing.apply(original, alice.id, edit.copy(content = "a".repeat(1000)), now).isSuccess())
    }

    @Test
    fun staleFetchAndOutOfOrderEchoCannotOverwriteEditedContentOrReportMetadata() {
        val updated = original.copy(content = "Updated", editedAt = now, senderName = "Student", isEditLocked = null)
        val latest = updated.copy(content = "Newest", editedAt = now + 1.minutes)
        val known = original.copy(senderName = "Alice").report()
        val merged = MessageTimeline.merge(
            listOf(latest, known),
            listOf(updated, original.copy(isEditLocked = null))
        ).single()
        assertEquals("Newest", merged.content)
        assertEquals("Alice", merged.senderName)
        assertTrue(merged.isReported)
        assertEquals(true, merged.isEditLocked)
    }

    @Test
    fun authoritativeDismissalClearsEditLockWhileRealtimePreservesKnownModeration() {
        val reported = original.report()
        val dismissed = reported.copy(isEditLocked = false)
        val merged = MessageTimeline.merge(listOf(reported), listOf(dismissed)).single()
        assertEquals(false, merged.isEditLocked)
        assertTrue(merged.isReported)
        val live = dismissed.copy(isEditLocked = null, content = "Changed", editedAt = now)
        assertEquals(false, MessageTimeline.merge(listOf(merged), listOf(live)).single().isEditLocked)
    }

    @Test
    fun editUseCaseRequiresCurrentIdentityAndValidatesBeforeWriting() = runTest {
        val repository = messagesRepository(messages = listOf(original), now = { now })
        val provider = MutableMessageSenderProvider()
        val action = EditMessage(repository, provider)
        assertIs<Result.Error>(action(edit.copy(content = " ")))
        val failure = Result.Error(IllegalStateException("Signed out"))
        provider.result = failure
        assertEquals(failure, action(edit))
        assertEquals(original, repository.getMessagePage(alice.id, conversationId).getOrThrow().messages.single())
    }

    private fun assertFailure(
        expected: MessageEditFailure,
        message: com.jdrms.bulletin.domain.messages.domain.model.Message,
        request: MessageEdit = edit,
        at: Instant = now
    ) {
        val result = MessageEditing.apply(message, alice.id, request, at)
        assertEquals(expected, assertIs<MessageEditException>(assertIs<Result.Error>(result).exception).failure)
    }
}
