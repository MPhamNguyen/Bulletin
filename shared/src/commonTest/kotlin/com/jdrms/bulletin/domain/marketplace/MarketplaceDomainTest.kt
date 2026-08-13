package com.jdrms.bulletin.domain.marketplace

import com.jdrms.bulletin.domain.marketplace.domain.model.*
import com.jdrms.bulletin.domain.marketplace.domain.service.ListingPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MarketplaceDomainTest {

    private val policy = ListingPolicy()

    @Test
    fun testValidPriceFormatting() {
        val price = Price(49.99)
        assertEquals("$49.99", price.formatted)
    }

    @Test
    fun testNegativePriceThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            Price(-10.0)
        }
    }

    @Test
    fun testListingPolicyValidation() {
        val validListing = Listing(
            id = ListingId("item_test"),
            sellerId = SellerId("seller_1"),
            sellerName = "Dominic",
            title = "Valid Textbook Title",
            description = "Condition is like new",
            price = Price(25.0),
            category = Category.TEXTBOOKS
        )
        val result = policy.validateListing(validListing)
        assertTrue(result.isSuccess())
    }

    @Test
    fun testShortTitleFailsValidation() {
        val invalidListing = Listing(
            id = ListingId("item_test"),
            sellerId = SellerId("seller_1"),
            sellerName = "Dominic",
            title = "Hi",
            description = "Condition is like new",
            price = Price(25.0),
            category = Category.TEXTBOOKS
        )
        val result = policy.validateListing(invalidListing)
        assertTrue(result.isError())
    }
}
