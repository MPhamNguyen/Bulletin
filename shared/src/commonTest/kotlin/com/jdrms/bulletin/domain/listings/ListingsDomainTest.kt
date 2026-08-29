package com.jdrms.bulletin.domain.listings

import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.service.ListingValidationPolicy
import com.jdrms.bulletin.domain.listings.infrastructure.dto.ListingDto
import com.jdrms.bulletin.domain.listings.infrastructure.mapper.ListingMapper
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ListingsDomainTest {

    private val policy = ListingValidationPolicy()

    @Test
    fun testValidPriceFormatting() {
        val price = ListingPrice(19.99)
        assertEquals("$19.99", price.formatted)
    }

    @Test
    fun testNegativePriceThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            ListingPrice(-10.0)
        }
    }

    @Test
    fun testShortTitleThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            Listing(
                id = ListingId("list_short"),
                sellerId = SellerId("s1"),
                sellerName = "Seller",
                title = "No",
                description = "Desc",
                price = ListingPrice(10.0),
                category = ListingCategory.OTHER
            )
        }
    }

    @Test
    fun testListingValidationPolicy() {
        val validListing = Listing(
            id = ListingId("list_valid"),
            sellerId = SellerId("s1"),
            sellerName = "Dominic",
            title = "CECS 491 Project Guide",
            description = "Complete reference manual",
            price = ListingPrice(15.0),
            category = ListingCategory.TEXTBOOKS
        )
        val result = policy.validateListing(validListing)
        assertTrue(result.isSuccess())
    }

    @Test
    fun testCreateAndManageListingInRepository() = runTest {
        val repo = InMemoryListingsRepository(initialListings = emptyList())
        val newListing = Listing(
            id = ListingId("list_test_1"),
            sellerId = SellerId("seller_99"),
            sellerName = "Tester",
            title = "Wireless Keyboard",
            description = "Mechanical switches, quiet typing",
            price = ListingPrice(35.0),
            category = ListingCategory.ELECTRONICS,
            condition = ListingCondition.LIKE_NEW
        )

        val createResult = repo.createListing(newListing)
        assertTrue(createResult.isSuccess())

        val sellerListings = repo.getSellerListings(SellerId("seller_99"))
        assertEquals(1, sellerListings.size)
        assertEquals("Wireless Keyboard", sellerListings.first().title)

        val deleteResult = repo.deleteListing(ListingId("list_test_1"))
        assertTrue(deleteResult.isSuccess())
        assertEquals(0, repo.getSellerListings(SellerId("seller_99")).size)
    }

    @Test
    fun testMapperRoundTrip() {
        val dto = ListingDto(
            id = "dto_list_1",
            sellerId = "s_1",
            sellerName = "Dominic",
            title = "Desk Chair",
            description = "Mesh back",
            price = 45.0,
            category = "FURNITURE",
            condition = "LIKE_NEW",
            status = "AVAILABLE"
        )
        val domain = ListingMapper.toDomain(dto)
        assertEquals(ListingCategory.FURNITURE, domain.category)
        assertEquals(ListingCondition.LIKE_NEW, domain.condition)
        assertEquals(ListingStatus.AVAILABLE, domain.status)

        val backToDto = ListingMapper.toDto(domain)
        assertEquals(dto.id, backToDto.id)
        assertEquals(dto.condition, backToDto.condition)
    }
}
