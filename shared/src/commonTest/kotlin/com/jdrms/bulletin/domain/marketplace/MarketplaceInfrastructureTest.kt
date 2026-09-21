package com.jdrms.bulletin.domain.marketplace

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.MarketplaceListingDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.mapper.MarketplaceMapper
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.SupabaseMarketplaceRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarketplaceInfrastructureTest {

    @Test
    fun testListingDtoMapperRoundTrip() {
        val dto = MarketplaceListingDto(
            id = "mkt_map_1",
            sellerId = "seller_99",
            sellerName = "Sean Gallagher",
            title = "Graphing Calculator TI-84",
            description = "Works great, fresh batteries included",
            price = 60.0,
            category = "ELECTRONICS",
            condition = "LIKE_NEW",
            status = "AVAILABLE",
            photos = listOf("https://example.com/calc.jpg"),
            reputationScore = 4.9,
            createdAtMillis = 1000L
        )

        val domain = MarketplaceMapper.toListingDomain(dto, isSaved = true, reputationScore = 4.9)
        assertEquals("mkt_map_1", domain.id.value)
        assertEquals("Sean Gallagher", domain.sellerName)
        assertEquals(MarketplaceCategory.ELECTRONICS, domain.category)
        assertEquals(4.9, domain.sellerReputationScore)
        assertEquals(true, domain.isSaved)
        assertEquals(1, domain.photos.size)

        val backToDto = MarketplaceMapper.toListingDto(domain)
        assertEquals(dto.id, backToDto.id)
        assertEquals(dto.title, backToDto.title)
        assertEquals(dto.price, backToDto.price)
        assertEquals(dto.category, backToDto.category)
        assertEquals(dto.reputationScore, backToDto.reputationScore)
    }

    @Test
    fun testInMemoryMarketplaceRepositoryViewListing() = runTest {
        val repo = InMemoryMarketplaceRepository()
        val result = repo.viewListing("mkt_1")
        assertTrue(result.isSuccess())

        val listing = (result as com.jdrms.bulletin.core.common.Result.Success).data
        assertEquals("mkt_1", listing.id.value)
        assertEquals("Dominic Alfonso", listing.sellerName)
        assertEquals(4.8, listing.sellerReputationScore)
        assertTrue(listing.photos.isNotEmpty())
    }

    @Test
    fun testInMemoryMarketplaceRepositoryViewListingNotFound() = runTest {
        val repo = InMemoryMarketplaceRepository()
        val result = repo.viewListing("nonexistent")
        assertTrue(result.isError())
    }

    @Test
    fun testErrorMessageMappingRules() {
        val tableError = Exception("Could not find the table `public.listings` in the schema cache")
        val tableMsg = SupabaseMarketplaceRepository.mapMarketplaceErrorMessage(tableError)
        assertEquals("Database table 'listings' not found. Please verify your Supabase schema setup.", tableMsg)

        val timeoutError = Exception("Unable to resolve host: connection timeout")
        val timeoutMsg = SupabaseMarketplaceRepository.mapMarketplaceErrorMessage(timeoutError)
        assertEquals("Unable to connect to server. Please check your internet connection.", timeoutMsg)

        val authError = Exception("JWT expired or invalid api key")
        val authMsg = SupabaseMarketplaceRepository.mapMarketplaceErrorMessage(authError)
        assertEquals("Unauthorized database request. Please check your Supabase API credentials.", authMsg)

        val rlsError = Exception("new row violates row-level security policy for table listings")
        val rlsMsg = SupabaseMarketplaceRepository.mapMarketplaceErrorMessage(rlsError)
        assertEquals("Database permission denied. Please check your Supabase RLS policies.", rlsMsg)
    }
}
