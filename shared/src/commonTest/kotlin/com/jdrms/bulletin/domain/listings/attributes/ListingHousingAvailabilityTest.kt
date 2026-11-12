package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeCatalog
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeKeys
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributes
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeValidationPolicy
import com.jdrms.bulletin.domain.listings.presentation.CreateListingStep
import com.jdrms.bulletin.domain.listings.presentation.ListingAttributeDraft
import com.jdrms.bulletin.domain.listings.presentation.ListingsUiState
import com.jdrms.bulletin.domain.listings.presentation.canContinue
import com.jdrms.bulletin.domain.listings.presentation.validateEditListingDraft
import com.jdrms.bulletin.domain.listings.presentation.validateNewListingDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListingHousingAvailabilityTest {
    private val key = ListingAttributeKeys.AVAILABLE_DATE

    @Test
    fun catalogAndDomainPolicyRequireAHousingAvailabilityDate() {
        assertEquals(true, ListingAttributeCatalog.definition(ListingCategory.HOUSING, key)?.required)
        val issues = ListingAttributeValidationPolicy().validate(ListingCategory.HOUSING, ListingAttributes.EMPTY)
        assertEquals(listOf(key), issues.map { it.key })
        assertEquals("Available date is required.", issues.single().message)
    }

    @Test
    fun blankOrMissingDateCannotProduceHousingAttributes() {
        listOf(emptyMap(), mapOf(key to "  ")).forEach { inputs ->
            val result = ListingAttributeInputParser().parse(ListingCategory.HOUSING, inputs)
            assertNull(result.attributes)
            assertEquals("Available date is required.", result.errors[key])
        }
    }

    @Test
    fun housingDetailsRequiresACompleteValidDateBeforeContinuing() {
        listOf("", "2026-09", "2026-02-30").forEach { date ->
            val state = ListingsUiState(
                newCategory = ListingCategory.HOUSING,
                newAttributes = ListingAttributeDraft(inputs = mapOf(key to date))
            )
            assertFalse(CreateListingStep.DETAILS.canContinue(state))
        }
        assertTrue(
            CreateListingStep.DETAILS.canContinue(
                ListingsUiState(
                    newCategory = ListingCategory.HOUSING,
                    newAttributes = ListingAttributeDraft(inputs = mapOf(key to "2026-09-01"))
                )
            )
        )
    }

    @Test
    fun creationAndEditingReportTheMissingDateAgainstItsField() {
        val state = ListingsUiState(
            newCategory = ListingCategory.HOUSING,
            newPriceCents = "125000",
            newTitle = "Campus room",
            newDescription = "Utilities included",
            editingListing = attributeListing(category = ListingCategory.HOUSING, attributes = housingAttributes),
            editCategory = ListingCategory.HOUSING,
            editPriceCents = "125000",
            editTitle = "Campus room",
            editDescription = "Utilities included"
        )
        val create = validateNewListingDraft(state)
        val edit = validateEditListingDraft(state)
        assertNull(create.draft)
        assertNull(edit.draft)
        assertEquals("Available date is required.", create.attributeErrors[key])
        assertEquals("Available date is required.", edit.attributeErrors[key])
    }

    @Test
    fun nonHousingDetailsRemainOptional() {
        ListingCategory.entries.filterNot { it == ListingCategory.HOUSING }.forEach { category ->
            assertTrue(CreateListingStep.DETAILS.canContinue(ListingsUiState(newCategory = category)))
        }
    }
}
