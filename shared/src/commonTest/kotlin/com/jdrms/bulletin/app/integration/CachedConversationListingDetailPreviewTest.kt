package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.PublicProfilePhotoRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.ProfileReadCache
import com.jdrms.bulletin.domain.profile.infrastructure.repository.PublicProfilePhotoCache
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabasePublicProfilePhotoRepository
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CachedConversationListingDetailPreviewTest {
    @Test
    fun inboxAvatarIsReusedWithoutAnotherCacheOrDatabaseLookup() = runTest {
        val listing = InMemoryListingsRepository().getAllListings().first()
        val participant = ConversationParticipant(
            SenderId(listing.sellerId.value),
            "Seller",
            avatarUrl = "https://example.com/inbox.jpg"
        )
        val conversation = Conversation(ConversationId("chat"), listOf(participant))
        val photos = PublicProfilePhotoRepository { error("No photo request should be made") }
        val preview = assertNotNull(
            listing.toConversationPreview().toCachedMarketplaceDetailPreview(
                listing.id.value,
                photos,
                listOf(conversation)
            )
        )
        assertEquals(participant.avatarUrl, preview.sellerAvatarUrl)
    }

    @Test
    fun previewReusesMessagesPhotoAndNeverQueriesForMissingOrExpiredPhotos() = runTest {
        var requests = 0
        var now = 0L
        val sellerId = UserId("00000000-0000-0000-0000-000000000001")
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine {
                requests++
                respond(
                    "[{\"avatar_url\":\"https://example.com/avatar.jpg\"}]",
                    headers = headersOf("Content-Type", "application/json")
                )
            }
        }
        try {
            val photos = SupabasePublicProfilePhotoRepository(client, ProfileReadCache { now }) { "viewer" }
            val listing = InMemoryListingsRepository().getAllListings().first()
            val card = listing.toConversationPreview()
            val snapshot = assertNotNull(card.details)
            val sellerCard = card.copy(details = snapshot.copy(sellerId = sellerId.value))
            val id = listing.id.value
            assertNull(assertNotNull(sellerCard.toCachedMarketplaceDetailPreview(id, photos)).sellerAvatarUrl)
            assertEquals(0, requests)
            photos.getAvatarUrl(sellerId).getOrThrow()
            repeat(2) {
                val preview = assertNotNull(sellerCard.toCachedMarketplaceDetailPreview(id, photos))
                assertEquals("https://example.com/avatar.jpg", preview.sellerAvatarUrl)
                assertEquals(listing.title, preview.content.title)
            }
            assertEquals(1, requests)
            val otherSeller = card.copy(details = snapshot.copy(sellerId = "other-seller"))
            assertNull(assertNotNull(otherSeller.toCachedMarketplaceDetailPreview(id, photos)).sellerAvatarUrl)
            now = PublicProfilePhotoCache.TTL_MILLIS
            assertNull(assertNotNull(sellerCard.toCachedMarketplaceDetailPreview(id, photos)).sellerAvatarUrl)
            assertEquals(1, requests)
        } finally {
            client.close()
        }
    }
}
