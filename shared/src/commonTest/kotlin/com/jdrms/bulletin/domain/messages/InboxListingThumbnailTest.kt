package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.ImageCropSettings
import com.jdrms.bulletin.core.common.withCropSettings
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationLockReason
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationInboxDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseInboxListingDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseInboxListingImageDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseInboxParticipantDto
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.SupabaseMessagesMapper
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class InboxListingThumbnailTest {
    @Test
    fun repositoriesAndConversationUpdatesPreserveTheThumbnailAndSoldLock() = runTest {
        val image = "https://example.com/cover.jpg".withCropSettings(ImageCropSettings(1.5f, 0.2f, -0.3f))
        val chat = Conversation(
            conversationId,
            conversation().participants,
            listingId = ListingReferenceId("listing"),
            isListingSold = true,
            listingThumbnailUrl = image
        )
        val repositories: List<MessagesRepository> = listOf(
            messagesRepository(listOf(chat)),
            SupabaseMessagesRepository(FakeSupabaseMessagesTable(listOf(chat)))
        )
        for (repository in repositories) {
            val loaded = repository.getConversationPage(alice.id, 0, 10).getOrThrow().single()
            val snapshots = listOf(
                loaded,
                repository.getConversation(alice.id, conversationId).getOrThrow(),
                loaded.recordMessage(message()).getOrThrow(),
                loaded.recordReadAt(alice.id, Instant.fromEpochMilliseconds(100)).getOrThrow()
            )
            for (snapshot in snapshots) {
                assertEquals(image, snapshot?.listingThumbnailUrl)
                assertEquals(ConversationLockReason.LISTING_SOLD, snapshot?.lockReason)
            }
        }
    }

    @Test
    fun firstPositionImageBecomesAPublicUrlWithItsSavedCrop() {
        val row = row(
            SupabaseInboxListingDto(
                listOf(
                    SupabaseInboxListingImageDto("second.jpg", 2),
                    SupabaseInboxListingImageDto("cover.jpg", 0, 1.5f, 0.2f, -0.3f)
                )
            )
        )
        val mapped = SupabaseMessagesMapper.toInboxConversation(row) { "https://example.com/$it" }
        assertEquals(
            "https://example.com/cover.jpg".withCropSettings(ImageCropSettings(1.5f, 0.2f, -0.3f)),
            mapped.listingThumbnailUrl
        )
    }

    @Test
    fun unavailableListingsHaveNoThumbnailAndInvalidCropDoesNotBreakTheInbox() {
        assertNull(SupabaseMessagesMapper.toInboxConversation(row(null)).listingThumbnailUrl)
        assertNull(SupabaseMessagesMapper.toInboxConversation(row(SupabaseInboxListingDto())).listingThumbnailUrl)
        val invalid = row(SupabaseInboxListingDto(listOf(SupabaseInboxListingImageDto("cover.jpg", 0, 99f))))
        assertEquals(
            "cover.jpg".withCropSettings(ImageCropSettings()),
            SupabaseMessagesMapper.toInboxConversation(invalid).listingThumbnailUrl
        )
    }

    private fun row(listing: SupabaseInboxListingDto?) = SupabaseConversationInboxDto(
        id = conversationId.value,
        participants = listOf(SupabaseInboxParticipantDto(alice.id.value), SupabaseInboxParticipantDto(carol.id.value)),
        listing = listing
    )
}
