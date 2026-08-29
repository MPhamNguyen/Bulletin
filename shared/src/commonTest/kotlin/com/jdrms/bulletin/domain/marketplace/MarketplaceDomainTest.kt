package com.jdrms.bulletin.domain.marketplace

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePrice
import com.jdrms.bulletin.domain.marketplace.domain.service.MarketplaceSearchPolicy
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.MarketplaceItemDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.mapper.MarketplaceMapper
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MarketplaceDomainTest {

    private val searchPolicy = MarketplaceSearchPolicy()

    @Test
    fun testValidPriceFormatting() {
        val price = MarketplacePrice(29.99)
        assertEquals("$29.99", price.formatted)
    }

    @Test
    fun testNegativePriceThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            MarketplacePrice(-1.0)
        }
    }

    @Test
    fun testBlankTitleThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            MarketplaceItem(
                id = MarketplaceItemId("m1"),
                sellerId = "s1",
                sellerName = "Seller",
                title = "   ",
                description = "Desc",
                price = MarketplacePrice(10.0),
                category = MarketplaceCategory.OTHER
            )
        }
    }

    @Test
    fun testSearchPolicyFiltering() {
        val item1 = MarketplaceItem(
            id = MarketplaceItemId("m1"),
            sellerId = "s1",
            sellerName = "Dominic",
            title = "Physics 151 Textbook",
            description = "Mechanics & Waves",
            price = MarketplacePrice(30.0),
            category = MarketplaceCategory.TEXTBOOKS
        )
        val item2 = MarketplaceItem(
            id = MarketplaceItemId("m2"),
            sellerId = "s2",
            sellerName = "Sean",
            title = "USB-C Hub",
            description = "Multi-port adapter",
            price = MarketplacePrice(15.0),
            category = MarketplaceCategory.ELECTRONICS
        )

        val items = listOf(item1, item2)

        val queryResults = searchPolicy.filterItems(items, "physics", null)
        assertEquals(1, queryResults.size)
        assertEquals("m1", queryResults.first().id.value)

        val categoryResults = searchPolicy.filterItems(items, "", MarketplaceCategory.ELECTRONICS)
        assertEquals(1, categoryResults.size)
        assertEquals("m2", categoryResults.first().id.value)
    }

    @Test
    fun testToggleSavedItem() = runTest {
        val repo = InMemoryMarketplaceRepository()
        val itemId = MarketplaceItemId("mkt_1")

        val saveResult = repo.toggleSaved("student_user", itemId)
        assertTrue(saveResult.isSuccess())
        assertTrue((saveResult as com.jdrms.bulletin.core.common.Result.Success).data)

        val savedIds = repo.getSavedItemIds("student_user")
        assertTrue(savedIds.contains(itemId))

        val unsaveResult = repo.toggleSaved("student_user", itemId)
        assertTrue(unsaveResult.isSuccess())
        assertEquals(false, (unsaveResult as com.jdrms.bulletin.core.common.Result.Success).data)
    }

    @Test
    fun testMapperRoundTrip() {
        val dto = MarketplaceItemDto(
            id = "mkt_100",
            sellerId = "s_100",
            sellerName = "Dominic",
            title = "Monitor",
            description = "4K display",
            price = 150.0,
            category = "ELECTRONICS"
        )
        val domain = MarketplaceMapper.toDomain(dto, isSaved = true)
        assertEquals(true, domain.isSaved)
        assertEquals(MarketplaceCategory.ELECTRONICS, domain.category)

        val backToDto = MarketplaceMapper.toDto(domain)
        assertEquals(dto.id, backToDto.id)
        assertEquals(dto.price, backToDto.price)
    }
}
