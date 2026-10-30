package com.jdrms.bulletin.domain.marketplace.attributes

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.application.pageFor
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeValue
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributes
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class MarketplaceAttributeFiltersTest {
    private fun laptop(
        id: String,
        createdAt: Long,
        brand: String?,
        ram: Long?,
        storage: Long? = null,
        device: String = "laptop"
    ) =
        MarketplaceListingSnapshot(
            id = id, sellerId = "s", sellerName = "S", title = "Item $id", description = "d",
            priceAmount = 10.0, priceCurrency = "USD", category = MarketplaceCategory.ELECTRONICS,
            createdAtMillis = createdAt,
            attributes = MarketplaceAttributes(
                listOfNotNull(
                    brand?.let { "brand" to MarketplaceAttributeValue.Text(it) },
                    "device_type" to MarketplaceAttributeValue.Choice(device),
                    ram?.let { "ram_gb" to MarketplaceAttributeValue.Integer(it) },
                    storage?.let { "storage_gb" to MarketplaceAttributeValue.Integer(it) }
                ).toMap()
            )
        )

    private val catalog = listOf(
        laptop("a", 3, "Framework", 16, 512),
        laptop("b", 2, "framework", 8, 256),
        laptop("c", 2, "Dell", 32, 1024),
        laptop("d", 1, null, null),
        laptop("e", 1, "Apple", 16, null, device = "phone")
    )

    private fun page(
        filters: MarketplaceAttributeFilters,
        pageSize: Int = 20,
        cursor: com.jdrms.bulletin.domain.marketplace.application.MarketplacePageCursor? = null
    ) =
        catalog.pageFor(
            MarketplacePageRequest(
                category = MarketplaceCategory.ELECTRONICS,
                attributeFilters = filters,
                pageSize = pageSize,
                cursor = cursor
            )
        )

    @Test
    fun deviceTypeAndBrandUseEqualityWithCanonicalBrandMatching() {
        assertEquals(listOf("e"), page(MarketplaceAttributeFilters.create(deviceType = "phone")).listings.map { it.id })
        assertEquals(
            listOf("a", "b"),
            page(MarketplaceAttributeFilters.create(brandText = " FRAMEWORK ")).listings.map { it.id }
        )
        assertEquals(emptyList(), page(MarketplaceAttributeFilters.create(brandText = "Frame")).listings)
    }

    @Test
    fun capacityRangesAreInclusiveAndAcceptOpenEnds() {
        fun ids(range: MarketplaceCapacityRange) = page(
            MarketplaceAttributeFilters(ramGb = range)
        ).listings.map { it.id }
        assertEquals(listOf("a", "e"), ids(MarketplaceCapacityRange(16, 16)))
        assertEquals(listOf("a", "c", "e"), ids(MarketplaceCapacityRange(min = 16)))
        assertEquals(listOf("b"), ids(MarketplaceCapacityRange(max = 8)))
    }

    @Test
    fun combinedFiltersAreAnded() {
        val filters = MarketplaceAttributeFilters.create(
            deviceType = "laptop",
            brandText = "framework",
            ramGb = MarketplaceCapacityRange(min = 16),
            storageGb = MarketplaceCapacityRange(min = 512)
        )
        assertEquals(listOf("a"), page(filters).listings.map { it.id })
    }

    @Test
    fun listingsMissingAFilteredAttributeNeverMatchButUnfilteredSearchKeepsThem() {
        assertEquals(5, page(MarketplaceAttributeFilters.NONE).listings.size)
        assertEquals(
            false,
            page(MarketplaceAttributeFilters(ramGb = MarketplaceCapacityRange(min = 1))).listings.any { it.id == "d" }
        )
        assertEquals(
            false,
            page(
                MarketplaceAttributeFilters(storageGb = MarketplaceCapacityRange(min = 1))
            ).listings.any { it.id in setOf("d", "e") }
        )
    }

    @Test
    fun paginationWithFiltersAndEqualTimestampsIsStable() {
        val filters = MarketplaceAttributeFilters.create(brandText = "framework")
        val first = page(filters, pageSize = 1)
        assertEquals(listOf("a"), first.listings.map { it.id })
        val second = page(filters, pageSize = 1, cursor = first.nextCursor)
        assertEquals(listOf("b"), second.listings.map { it.id })
        assertNull(second.nextCursor)
        val allEqual = listOf(laptop("x1", 5, "Dell", 8), laptop("x2", 5, "Dell", 8), laptop("x3", 5, "Dell", 8))
        val request = MarketplacePageRequest(
            category = MarketplaceCategory.ELECTRONICS,
            attributeFilters = MarketplaceAttributeFilters.create(brandText = "dell"),
            pageSize = 2
        )
        val p1 = allEqual.pageFor(request)
        val p2 = allEqual.pageFor(request.copy(cursor = p1.nextCursor))
        assertEquals(listOf("x3", "x2", "x1"), p1.listings.map { it.id } + p2.listings.map { it.id })
    }

    @Test
    fun attributeTextDoesNotContributeToFreeTextSearch() {
        val request = MarketplacePageRequest(query = "framework", category = MarketplaceCategory.ELECTRONICS)
        assertEquals(emptyList(), catalog.pageFor(request).listings)
    }

    @Test
    fun invalidFiltersAreRejected() {
        assertFailsWith<IllegalArgumentException> { MarketplaceAttributeFilters(deviceType = "toaster") }
        assertFailsWith<IllegalArgumentException> { MarketplaceAttributeFilters(brandKey = "Not Normalized!") }
        assertFailsWith<IllegalArgumentException> { MarketplaceAttributeFilters(brandKey = "") }
        assertFailsWith<IllegalArgumentException> { MarketplaceCapacityRange(null, null) }
        assertFailsWith<IllegalArgumentException> { MarketplaceCapacityRange(64, 8) }
        assertFailsWith<IllegalArgumentException> { MarketplaceCapacityRange(min = -1) }
        assertFailsWith<IllegalArgumentException> {
            MarketplacePageRequest(
                category = MarketplaceCategory.TEXTBOOKS,
                attributeFilters = MarketplaceAttributeFilters.create(deviceType = "laptop")
            )
        }
        assertFailsWith<IllegalArgumentException> {
            MarketplacePageRequest(
                category = null,
                attributeFilters = MarketplaceAttributeFilters.create(brandText = "dell")
            )
        }
    }

    @Test
    fun equivalentFiltersShareRequestIdentityAndDifferentOnesDoNot() {
        fun request(filters: MarketplaceAttributeFilters) = MarketplacePageRequest(
            category = MarketplaceCategory.ELECTRONICS,
            attributeFilters = filters
        )
        assertEquals(
            request(MarketplaceAttributeFilters.create(brandText = "  Framework ")),
            request(MarketplaceAttributeFilters.create(brandText = "FRAMEWORK"))
        )
        assertNotEquals(
            request(MarketplaceAttributeFilters.create(brandText = "dell")),
            request(MarketplaceAttributeFilters.create(brandText = "hp"))
        )
        assertNotEquals(
            request(MarketplaceAttributeFilters.NONE),
            request(MarketplaceAttributeFilters.create(deviceType = "laptop"))
        )
        assertNotEquals(
            request(MarketplaceAttributeFilters(ramGb = MarketplaceCapacityRange(8, 16))),
            request(MarketplaceAttributeFilters(ramGb = MarketplaceCapacityRange(8, 32)))
        )
    }
}
