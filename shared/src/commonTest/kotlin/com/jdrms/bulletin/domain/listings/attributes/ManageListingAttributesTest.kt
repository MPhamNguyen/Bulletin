package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.application.MarkListingSold
import com.jdrms.bulletin.domain.listings.application.RestoreListingToMarketplace
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributes
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.service.ListingValidationException
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ManageListingAttributesTest {
    private val seller = SellerId("seller-1")

    private suspend fun repositoryWith(listing: com.jdrms.bulletin.domain.listings.domain.model.Listing) =
        InMemoryListingsRepository(emptyList()).also { it.createListing(listing) }

    @Test
    fun attributeOnlyEditPersistsAndKeepsServerOwnedFields() = runTest {
        val stored = attributeListing(attributes = ListingAttributes.EMPTY)
            .copy(buyerId = "buyer", images = listOf("a.jpg"), createdAtMillis = 5L)
        val repository = repositoryWith(stored)
        val edit = stored.copy(
            attributes = laptopAttributes,
            buyerId = "forged",
            images = emptyList(),
            createdAtMillis = 99L
        )

        val result = ManageListing(repository).updateListing(edit, seller)

        assertIs<Result.Success<*>>(result)
        val saved = repository.getListing(stored.id)!!
        assertEquals(laptopAttributes, saved.attributes)
        assertEquals("buyer", saved.buyerId)
        assertEquals(listOf("a.jpg"), saved.images)
        assertEquals(5L, saved.createdAtMillis)
    }

    @Test
    fun nonOwnerCannotChangeAttributes() = runTest {
        val repository = repositoryWith(attributeListing())
        val edit = attributeListing(attributes = ListingAttributes.EMPTY)

        val result = ManageListing(repository).updateListing(edit, SellerId("intruder"))

        assertIs<ListingValidationException.Unauthorized>((result as Result.Error).exception)
        assertEquals(laptopAttributes, repository.getListing(ListingId("listing-1"))!!.attributes)
    }

    @Test
    fun invalidAttributesAreRejectedAndNothingIsSaved() = runTest {
        val repository = repositoryWith(attributeListing(attributes = ListingAttributes.EMPTY))
        val invalid =
            attributeListing(attributes = ListingAttributes(mapOf("ram_gb" to ListingAttributeValue.Integer(8))))

        val result = ManageListing(repository).updateListing(invalid, seller)

        assertIs<ListingValidationException.InvalidAttributes>((result as Result.Error).exception)
        assertTrue(repository.getListing(ListingId("listing-1"))!!.attributes.isEmpty())
    }

    @Test
    fun unknownStoredValuesSurviveUnrelatedEdits() = runTest {
        val withFuture = laptopAttributes.withPreserved(mapOf("future_key" to "\"kept\""))
        val repository = repositoryWith(attributeListing(attributes = withFuture))
        // An older client edits only the title and knows nothing about future_key.
        val edit = attributeListing(attributes = laptopAttributes).copy(title = "New title")

        ManageListing(repository).updateListing(edit, seller)

        val saved = repository.getListing(ListingId("listing-1"))!!
        assertEquals("New title", saved.title)
        assertEquals(mapOf("future_key" to "\"kept\""), saved.attributes.preserved)
    }

    @Test
    fun newUnknownKeysAreRejectedInNormalWrites() = runTest {
        val repository = repositoryWith(attributeListing(attributes = ListingAttributes.EMPTY))
        val edit = attributeListing(attributes = ListingAttributes(preserved = mapOf("sneaky" to "1")))

        val result = ManageListing(repository).updateListing(edit, seller)

        assertIs<ListingValidationException.UnknownAttributes>((result as Result.Error).exception)
        val create = CreateListing(InMemoryListingsRepository(emptyList())).invoke(edit)
        assertIs<ListingValidationException.UnknownAttributes>((create as Result.Error).exception)
    }

    @Test
    fun clearingAttributesLeavesPreservedValuesAlone() = runTest {
        val withFuture = laptopAttributes.withPreserved(mapOf("future_key" to "\"kept\""))
        val repository = repositoryWith(attributeListing(attributes = withFuture))
        val edit = attributeListing(attributes = ListingAttributes.EMPTY)

        ManageListing(repository).updateListing(edit, seller)

        val saved = repository.getListing(ListingId("listing-1"))!!
        assertTrue(saved.attributes.values.isEmpty())
        assertEquals(mapOf("future_key" to "\"kept\""), saved.attributes.preserved)
    }

    @Test
    fun legacyListingWithoutAttributesStaysEditable() = runTest {
        val legacy = attributeListing(attributes = ListingAttributes.EMPTY)
        val repository = repositoryWith(legacy)

        val result = ManageListing(repository).updateListing(legacy.copy(title = "Renamed legacy"), seller)

        assertIs<Result.Success<*>>(result)
    }

    @Test
    fun categoryChangeWithStaleAttributesIsRejectedUntilTheyAreRemoved() = runTest {
        val repository = repositoryWith(attributeListing())
        val stale = attributeListing(category = ListingCategory.CLOTHING)
        assertIs<ListingValidationException.InvalidAttributes>(
            (ManageListing(repository).updateListing(stale, seller) as Result.Error).exception
        )
        val clean = stale.copy(attributes = ListingAttributes(mapOf("brand" to laptopAttributes["brand"]!!)))
        assertIs<Result.Success<*>>(ManageListing(repository).updateListing(clean, seller))
    }

    @Test
    fun soldAndRestoredListingsKeepAttributes() = runTest {
        val repository = repositoryWith(attributeListing())
        MarkListingSold(repository)(ListingId("listing-1"), seller, "buyer")
        assertEquals(ListingStatus.SOLD, repository.getListing(ListingId("listing-1"))!!.status)
        assertEquals(laptopAttributes, repository.getListing(ListingId("listing-1"))!!.attributes)
        RestoreListingToMarketplace(repository)(ListingId("listing-1"), seller)
        assertEquals(laptopAttributes, repository.getListing(ListingId("listing-1"))!!.attributes)
    }
}
