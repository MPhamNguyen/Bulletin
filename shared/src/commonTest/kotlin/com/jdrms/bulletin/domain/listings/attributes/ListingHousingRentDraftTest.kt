package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeKeys
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingDecimal
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import com.jdrms.bulletin.domain.listings.presentation.ListingAttributeDraft
import com.jdrms.bulletin.domain.listings.presentation.ListingsUiState
import com.jdrms.bulletin.domain.listings.presentation.listingAttributeFormFields
import com.jdrms.bulletin.domain.listings.presentation.parseListingFormAttributes
import com.jdrms.bulletin.domain.listings.presentation.validateEditListingDraft
import com.jdrms.bulletin.domain.listings.presentation.validateNewListingDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListingHousingRentDraftTest {
    private val rent = ListingAttributeKeys.MONTHLY_RENT_USD
    private val stale = ListingAttributeDraft(inputs = mapOf(rent to "900", "housing_type" to "room"))

    @Test
    fun housingDetailsDoNotExposeASecondRentInput() {
        val keys = listingAttributeFormFields(ListingCategory.HOUSING, stale.inputs).map { it.definition.key }
        assertFalse(rent in keys)
        assertTrue("housing_type" in keys)
        assertTrue("available_date" in keys)
    }

    @Test
    fun creationUsesOneAmountForPriceAndRentReplacingStaleAttributeInput() {
        val state = ListingsUiState(
            newTitle = "Campus room",
            newDescription = "Utilities included",
            newCategory = ListingCategory.HOUSING,
            newPriceCents = "125050",
            newImages = listOf(ListingImageUpload(byteArrayOf(1), "image/jpeg")),
            newAttributes = stale
        )
        val draft = assertNotNull(validateNewListingDraft(state).draft)
        assertEquals(1250.50, draft.price)
        assertEquals(ListingAttributeValue.Decimal(ListingDecimal(125050, 2)), draft.attributes[rent])
        assertEquals(ListingAttributeValue.Choice("room"), draft.attributes["housing_type"])
        assertEquals(
            "Please enter the monthly rent",
            validateNewListingDraft(state.copy(newPriceCents = "")).errorMessage
        )
    }

    @Test
    fun editingReplacesOldRentMetadataUsingTheDisplayedPrice() {
        val state = ListingsUiState(
            editingListing = attributeListing(category = ListingCategory.HOUSING, attributes = housingAttributes),
            editTitle = "Campus apartment",
            editDescription = "Available next month",
            editCategory = ListingCategory.HOUSING,
            editPriceCents = "150000",
            editAttributes = stale.copy(preserved = mapOf("future" to "true", rent to "\"bad legacy value\""))
        )
        val draft = assertNotNull(validateEditListingDraft(state).draft)
        assertEquals(1500.0, draft.price)
        assertEquals(ListingAttributeValue.Decimal(ListingDecimal(150000, 2)), draft.attributes[rent])
        assertEquals(mapOf("future" to "true"), draft.attributes.preserved)
    }

    @Test
    fun rentBoundsProduceAnAmountErrorInsteadOfErrorsForAHiddenAttributeField() {
        listOf("0", "10000000").forEach { digits ->
            val result = parseListingFormAttributes(ListingCategory.HOUSING, stale, digits)
            assertNull(result.attributes)
            assertNotNull(result.rentError)
            assertFalse(rent in result.errors)
        }
        listOf("1", "9999999").forEach { digits ->
            assertNotNull(parseListingFormAttributes(ListingCategory.HOUSING, stale, digits).attributes)
        }
    }

    @Test
    fun rentIsNotAddedForOtherCategories() {
        val result = parseListingFormAttributes(ListingCategory.ELECTRONICS, ListingAttributeDraft(), "125050")
        assertNull(result.attributes?.get(rent))
        assertNull(result.rentError)
    }
}
