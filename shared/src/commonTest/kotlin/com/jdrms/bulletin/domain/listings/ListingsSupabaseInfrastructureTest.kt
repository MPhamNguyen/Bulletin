package com.jdrms.bulletin.domain.listings

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CreateListingErrorMessages
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingDto
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingInsertDto
import com.jdrms.bulletin.domain.listings.infrastructure.repository.SupabaseListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.SupabaseListingsTable
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListingsSupabaseInfrastructureTest {

    @Test
    fun createListingInsertsDomainFieldsAndPreservesUuid() = runTest {
        val table = FakeSupabaseListingsTable()
        val repository = SupabaseListingsRepository(table)
        val listing = testListing()

        val result = repository.createListing(listing)

        assertTrue(result.isSuccess())
        assertEquals(listing, (result as Result.Success).data)
        assertEquals(listing.id.value, table.inserted?.id)
        assertEquals(listing.sellerId.value, table.inserted?.userId)
        assertEquals(listing.title, table.inserted?.name)
    }

    @Test
    fun createListingReturnsDuplicateIdErrorBeforeInsert() = runTest {
        val table = FakeSupabaseListingsTable(existingId = "listing-uuid")
        val repository = SupabaseListingsRepository(table)

        val result = repository.createListing(testListing())

        assertTrue(result.isError())
        assertEquals(CreateListingErrorMessages.GENERIC_FAILURE, (result as Result.Error).message)
        assertEquals(null, table.inserted)
    }

    @Test
    fun createListingHidesUnexpectedRepositoryErrors() = runTest {
        val table = FakeSupabaseListingsTable(insertError = IllegalStateException("row-level security denied"))
        val repository = SupabaseListingsRepository(table)

        val result = repository.createListing(testListing())

        assertTrue(result.isError())
        assertEquals(CreateListingErrorMessages.GENERIC_FAILURE, (result as Result.Error).message)
    }

    private fun testListing(): Listing {
        return Listing(
            id = ListingId("listing-uuid"),
            sellerId = SellerId("seller-uuid"),
            sellerName = "Student Seller",
            title = "Wireless Keyboard",
            description = "Mechanical switches",
            price = ListingPrice(35.0),
            category = ListingCategory.ELECTRONICS,
            condition = ListingCondition.LIKE_NEW,
            status = ListingStatus.AVAILABLE,
            createdAtMillis = 123L
        )
    }

    private class FakeSupabaseListingsTable(
        private val existingId: String? = null,
        private val insertError: Throwable? = null
    ) : SupabaseListingsTable {
        var inserted: SupabaseListingInsertDto? = null

        override suspend fun findById(id: String): SupabaseListingDto? {
            return if (id == existingId) {
                SupabaseListingDto(id, "Existing", "seller-uuid", "OTHER", "GOOD", 1.0, "Existing")
            } else {
                null
            }
        }

        override suspend fun insert(listing: SupabaseListingInsertDto) {
            insertError?.let { throw it }
            inserted = listing
        }

        override suspend fun delete(id: String) = Unit

        override suspend fun getListings(sellerId: String?): List<SupabaseListingDto> = emptyList()

    }
}
