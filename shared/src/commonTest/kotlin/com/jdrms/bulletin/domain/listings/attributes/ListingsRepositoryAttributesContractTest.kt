package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributes
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingDto
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingInsertDto
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingWriteDto
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.SupabaseListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.SupabaseListingsTable
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The same observable attribute behavior must hold for every [ListingsRepository] implementation. */
class ListingsRepositoryAttributesContractTest {
    private class StatefulSupabaseTable : SupabaseListingsTable {
        val rows = linkedMapOf<String, SupabaseListingWriteDto>()
        override suspend fun findById(id: String) = rows[id]?.toReadDto()
        override suspend fun insert(listing: SupabaseListingInsertDto) { rows[listing.id] = listing }
        override suspend fun update(listing: SupabaseListingWriteDto) { rows[listing.id] = listing }
        override suspend fun delete(id: String, sellerId: String) { rows.remove(id) }
        override suspend fun getListings(sellerId: String?) =
            rows.values.filter { sellerId == null || it.userId == sellerId }.map { it.toReadDto() }

        private fun SupabaseListingWriteDto.toReadDto() = SupabaseListingDto(
            id = id, name = name, userId = userId, category = category, condition = condition, price = price,
            description = description, isSold = isSold, buyerId = buyerId, attributes = attributes
        )
    }

    private fun implementations(): Map<String, ListingsRepository> = mapOf(
        "in-memory" to InMemoryListingsRepository(emptyList()),
        "supabase" to SupabaseListingsRepository(StatefulSupabaseTable()) { }
    )

    @Test
    fun attributesRoundTripWithTypedValues() = runTest {
        implementations().forEach { (name, repository) ->
            repository.createListing(attributeListing())
            assertEquals(laptopAttributes, repository.getListing(ListingId("listing-1"))?.attributes, name)
            assertEquals(laptopAttributes, repository.getSellerListings(SellerId("seller-1")).single().attributes, name)
        }
    }

    @Test
    fun housingDecimalsAndDatesRoundTripExactly() = runTest {
        implementations().forEach { (name, repository) ->
            repository.createListing(
                attributeListing(
                    category = com.jdrms.bulletin.domain.listings.domain.model.ListingCategory.HOUSING,
                    attributes = housingAttributes
                )
            )
            assertEquals(housingAttributes, repository.getListing(ListingId("listing-1"))?.attributes, name)
        }
    }

    @Test
    fun updatingToEmptyClearsAttributesAndPreservedValuesStayStored() = runTest {
        implementations().forEach { (name, repository) ->
            val withFuture = laptopAttributes.withPreserved(mapOf("future_key" to "\"kept\""))
            repository.createListing(attributeListing(attributes = withFuture))
            repository.updateListing(attributeListing(attributes = ListingAttributes(preserved = withFuture.preserved)))
            val stored = repository.getListing(ListingId("listing-1"))!!.attributes
            assertTrue(stored.values.isEmpty(), name)
            assertEquals(withFuture.preserved, stored.preserved, name)
        }
    }

    @Test
    fun legacyListingsWithoutAttributesReadAsEmpty() = runTest {
        implementations().forEach { (name, repository) ->
            repository.createListing(attributeListing(attributes = ListingAttributes.EMPTY))
            assertEquals(ListingAttributes.EMPTY, repository.getListing(ListingId("listing-1"))?.attributes, name)
        }
    }
}
