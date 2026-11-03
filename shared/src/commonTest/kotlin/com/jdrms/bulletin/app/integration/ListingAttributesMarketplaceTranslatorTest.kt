package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeCatalog
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeType
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributes
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeText
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.application.pageFor
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeCatalog
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeKind
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeMatching
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeValue
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ListingAttributesMarketplaceTranslatorTest {
    @Test
    fun marketplaceVocabularyMatchesTheListingsCatalog() {
        ListingCategory.entries.forEach { category ->
            val listings = ListingAttributeCatalog.definitionsFor(category)
            val marketplace = MarketplaceAttributeCatalog.specsFor(MarketplaceCategory.valueOf(category.name))
            assertEquals(listings.map { it.key }, marketplace.map { it.key }, category.name)
            listings.zip(marketplace).forEach { (definition, spec) ->
                assertEquals(definition.label, spec.label)
                val type = definition.type
                if (type is ListingAttributeType.Choice) {
                    assertEquals(type.options.associate { it.code to it.label }, spec.choices, definition.key)
                    assertEquals(MarketplaceAttributeKind.CHOICE, spec.kind)
                }
            }
        }
    }

    @Test
    fun brandMatchingIsIdenticalInBothContexts() {
        listOf("Levi's", " H&M ", "Under-Armour", "DELL  Inc.", "Pen Tel", "Pentel", "3M", "Framework").forEach {
            assertEquals(ListingAttributeText.matchKey(it), MarketplaceAttributeMatching.brandKey(it), it)
        }
    }

    @Test
    fun translatesTypedValuesAndLeavesPreservedEntriesBehind() {
        val attributes = ListingAttributes(
            mapOf("ram_gb" to ListingAttributeValue.Integer(16), "brand" to ListingAttributeValue.Text("Dell")),
            preserved = mapOf("future" to "\"x\"")
        )
        val translated = attributes.toMarketplaceAttributes()
        assertEquals(MarketplaceAttributeValue.Integer(16), translated["ram_gb"])
        assertEquals(setOf("ram_gb", "brand"), translated.values.keys)
    }

    @Test
    fun editedAttributesAreVisibleToFiltersThroughTheListingSource() = runTest {
        val repository = InMemoryListingsRepository(emptyList())
        val laptop = Listing(
            id = ListingId("l1"),
            sellerId = SellerId("s1"),
            sellerName = "S",
            title = "Laptop",
            description = "d",
            price = ListingPrice(5.0),
            category = ListingCategory.ELECTRONICS,
            attributes = ListingAttributes(
                mapOf(
                    "device_type" to ListingAttributeValue.Choice("laptop"),
                    "ram_gb" to ListingAttributeValue.Integer(8)
                )
            )
        )
        repository.createListing(laptop)
        val source = ListingsMarketplaceListingSource(repository)
        val request = MarketplacePageRequest(
            category = MarketplaceCategory.ELECTRONICS,
            attributeFilters = MarketplaceAttributeFilters.create(deviceType = "laptop")
        )
        assertEquals(1, source.getAvailableListings(request).listings.pageFor(request).listings.size)
        repository.updateListing(laptop.copy(attributes = ListingAttributes.EMPTY))
        assertEquals(0, source.getAvailableListings(request).listings.pageFor(request).listings.size)
    }
}
