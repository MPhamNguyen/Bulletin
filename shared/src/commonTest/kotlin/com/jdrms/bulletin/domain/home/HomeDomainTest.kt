package com.jdrms.bulletin.domain.home

import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCategory
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.HomePrice
import com.jdrms.bulletin.domain.home.domain.model.UserPreferences
import com.jdrms.bulletin.domain.home.domain.service.HomeRankingPolicy
import com.jdrms.bulletin.domain.home.infrastructure.dto.HomeFeedItemDto
import com.jdrms.bulletin.domain.home.infrastructure.mapper.HomeMapper
import com.jdrms.bulletin.domain.home.infrastructure.repository.InMemoryHomeRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HomeDomainTest {

    private val rankingPolicy = HomeRankingPolicy()

    @Test
    fun testValidPriceFormatting() {
        val price = HomePrice(49.99)
        assertEquals("$49.99", price.formatted)
    }

    @Test
    fun testNegativePriceThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            HomePrice(-5.0)
        }
    }

    @Test
    fun testBlankTitleThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            HomeFeedItem(
                id = HomeItemId("h_1"),
                title = "   ",
                description = "Desc",
                price = HomePrice(10.0),
                category = HomeFeedCategory.TEXTBOOKS
            )
        }
    }

    @Test
    fun testRankingPolicyPrioritizesPreferredCategory() {
        val prefs = UserPreferences(
            preferredCategories = listOf(HomeFeedCategory.TEXTBOOKS),
            maxPrice = 50.0
        )
        val textbook = HomeFeedItem(
            id = HomeItemId("h_tb"),
            title = "Algorithms Book",
            description = "Good condition",
            price = HomePrice(30.0),
            category = HomeFeedCategory.TEXTBOOKS
        )
        val furniture = HomeFeedItem(
            id = HomeItemId("h_fn"),
            title = "Desk Lamp",
            description = "Bright LED",
            price = HomePrice(20.0),
            category = HomeFeedCategory.FURNITURE
        )

        val ranked = rankingPolicy.rankItems(listOf(furniture, textbook), prefs)
        assertEquals("h_tb", ranked.first().id.value)
        assertTrue(ranked.first().score > ranked.last().score)
    }

    @Test
    fun testRepositoryFeedAndPreferences() = runTest {
        val repo = InMemoryHomeRepository()
        val feed = repo.getFeed("student_1")
        assertTrue(feed.isNotEmpty())

        val newPrefs = UserPreferences(
            preferredCategories = listOf(HomeFeedCategory.ELECTRONICS),
            maxPrice = 100.0
        )
        val updateResult = repo.updatePreferences("student_1", newPrefs)
        assertTrue(updateResult.isSuccess())

        val updatedPrefs = repo.getPreferences("student_1")
        assertEquals(100.0, updatedPrefs.maxPrice)
        assertEquals(listOf(HomeFeedCategory.ELECTRONICS), updatedPrefs.preferredCategories)
    }

    @Test
    fun testMapperRoundTrip() {
        val dto = HomeFeedItemDto(
            id = "dto_1",
            title = "Backpack",
            description = "Waterproof",
            price = 25.0,
            category = "CLOTHING",
            score = 85.0,
            reason = "Popular"
        )
        val domain = HomeMapper.toDomain(dto)
        assertEquals(HomeFeedCategory.CLOTHING, domain.category)
        assertEquals("$25.0", domain.price.formatted)

        val backToDto = HomeMapper.toDto(domain)
        assertEquals(dto.id, backToDto.id)
        assertEquals(dto.title, backToDto.title)
    }
}
