package com.jdrms.bulletin.domain.listings

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.application.MarkListingSold
import com.jdrms.bulletin.domain.listings.application.RestoreListingToMarketplace
import com.jdrms.bulletin.domain.listings.attributes.attributeListing
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingUpdateIntent
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Which seller actions count as an intentional edit (advance `edited_at`) and which are bookkeeping. */
class ListingUpdateIntentUseCasesTest {
    private val owner = SellerId("seller-1")
    private var now = 1_000L

    private class RecordingRepository(
        private val delegate: ListingsRepository
    ) : ListingsRepository by delegate {
        val intents = mutableListOf<ListingUpdateIntent>()

        override suspend fun updateListing(listing: Listing, intent: ListingUpdateIntent): Result<Listing> {
            intents += intent
            return delegate.updateListing(listing, intent)
        }
    }

    private suspend fun fixture(): RecordingRepository {
        val repository = RecordingRepository(InMemoryListingsRepository(emptyList()) { now })
        repository.createListing(attributeListing().copy(createdAtMillis = 1_000L))
        return repository
    }

    private suspend fun RecordingRepository.stored() = getListing(ListingId("listing-1"))!!

    @Test
    fun saveEditAdvancesEditedAt() = runTest {
        val repository = fixture()
        now = 2_000L

        val result = ManageListing(repository).saveEdit(repository.stored().copy(title = "New title"), owner)

        assertTrue(result.isSuccess())
        assertEquals(listOf(ListingUpdateIntent.EDIT), repository.intents)
        assertEquals(2_000L, repository.stored().editedAtMillis)
    }

    @Test
    fun updateListingIsBookkeepingAndLeavesEditedAtAlone() = runTest {
        val repository = fixture()
        val before = repository.stored().editedAtMillis
        now = 2_000L

        ManageListing(repository).updateListing(repository.stored().copy(images = listOf("a.jpg")), owner)

        assertEquals(listOf(ListingUpdateIntent.ROUTINE), repository.intents)
        assertEquals(before, repository.stored().editedAtMillis)
    }

    @Test
    fun aRejectedEditWritesNothingAndLeavesEditedAtAlone() = runTest {
        val repository = fixture()
        val before = repository.stored().editedAtMillis
        now = 2_000L

        val hijack = repository.stored().copy(title = "Hijack")

        val result = ManageListing(repository).saveEdit(hijack, SellerId("intruder"))

        assertTrue(result.isError())
        assertEquals(emptyList(), repository.intents)
        assertEquals(before, repository.stored().editedAtMillis)
    }

    @Test
    fun markingSoldAndRestoringAreEdits() = runTest {
        val repository = fixture()
        now = 2_000L
        MarkListingSold(repository)(ListingId("listing-1"), owner)
        assertEquals(2_000L, repository.stored().editedAtMillis)

        now = 3_000L
        RestoreListingToMarketplace(repository)(ListingId("listing-1"), owner)
        assertEquals(3_000L, repository.stored().editedAtMillis)
        assertEquals(listOf(ListingUpdateIntent.EDIT, ListingUpdateIntent.EDIT), repository.intents)
    }

    @Test
    fun aRejectedStatusChangeDoesNotAdvanceEditedAt() = runTest {
        val repository = fixture()
        val before = repository.stored().editedAtMillis
        now = 2_000L

        val result = MarkListingSold(repository)(ListingId("listing-1"), SellerId("intruder"))

        assertTrue(result.isError())
        assertEquals(emptyList(), repository.intents)
        assertEquals(before, repository.stored().editedAtMillis)
    }
}
