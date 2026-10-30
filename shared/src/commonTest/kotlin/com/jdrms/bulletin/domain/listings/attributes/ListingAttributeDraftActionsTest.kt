package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributes
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import com.jdrms.bulletin.domain.listings.presentation.ListingAttributeDisplay
import com.jdrms.bulletin.domain.listings.presentation.ListingAttributeDraft
import com.jdrms.bulletin.domain.listings.presentation.ListingAttributeDraftActions
import com.jdrms.bulletin.domain.listings.presentation.ListingAttributeSuggestions
import com.jdrms.bulletin.domain.listings.presentation.ListingForm
import com.jdrms.bulletin.domain.listings.presentation.ListingsUiState
import com.jdrms.bulletin.domain.listings.presentation.validateEditListingDraft
import com.jdrms.bulletin.domain.listings.presentation.validateNewListingDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListingAttributeDraftActionsTest {
    private val state = MutableStateFlow(
        ListingsUiState(
            newCategory = ListingCategory.ELECTRONICS,
            newTitle = "Laptop",
            newDescription = "Nice",
            newPriceCents = "10000",
            newImages = listOf(ListingImageUpload(byteArrayOf(1), "image/jpeg"))
        )
    )
    private val actions = ListingAttributeDraftActions(state)

    @Test
    fun categoryChangeWithoutValuesAppliesImmediately() {
        actions.selectCategory(ListingForm.NEW, ListingCategory.CLOTHING)
        assertEquals(ListingCategory.CLOTHING, state.value.newCategory)
        assertNull(state.value.pendingCategoryChange)
    }

    @Test
    fun categoryChangeThatDiscardsValuesWaitsForConfirmation() {
        actions.inputChanged(ListingForm.NEW, "device_type", "laptop")
        actions.inputChanged(ListingForm.NEW, "brand", "Framework")
        actions.selectCategory(ListingForm.NEW, ListingCategory.CLOTHING)

        val pending = assertNotNull(state.value.pendingCategoryChange)
        assertEquals(listOf("device_type"), pending.discarded.map { it.key })
        assertEquals(ListingCategory.ELECTRONICS, state.value.newCategory)

        actions.confirmCategoryChange()
        assertEquals(ListingCategory.CLOTHING, state.value.newCategory)
        assertEquals(mapOf("brand" to "Framework"), state.value.newAttributes.inputs)
        assertNull(state.value.pendingCategoryChange)
    }

    @Test
    fun cancellingLeavesCategoryAndDraftUnchanged() {
        actions.inputChanged(ListingForm.NEW, "device_type", "laptop")
        val before = state.value.copy(pendingCategoryChange = null)
        actions.selectCategory(ListingForm.NEW, ListingCategory.HOUSING)
        actions.cancelCategoryChange()
        assertEquals(before, state.value)
    }

    @Test
    fun submittedValuesFollowTheVisibleCategory() {
        actions.inputChanged(ListingForm.NEW, "brand", "Dell")
        actions.inputChanged(ListingForm.NEW, "device_type", "laptop")
        actions.inputChanged(ListingForm.NEW, "ram_gb", "16")
        assertEquals(
            setOf("brand", "device_type", "ram_gb"),
            validateNewListingDraft(state.value).draft?.attributes?.values?.keys
        )
        actions.selectCategory(ListingForm.NEW, ListingCategory.TEXTBOOKS)
        actions.confirmCategoryChange()
        assertEquals(emptySet(), validateNewListingDraft(state.value).draft?.attributes?.values?.keys)
    }

    @Test
    fun editAndNewDraftsAreIndependent() {
        actions.inputChanged(ListingForm.EDIT, "brand", "Apple")
        assertTrue(state.value.newAttributes.inputs.isEmpty())
        assertEquals("Apple", state.value.editAttributes.inputs["brand"])
    }

    @Test
    fun invalidAttributesKeepInputAndReportInlineErrorsPerField() {
        actions.inputChanged(ListingForm.NEW, "device_type", "laptop")
        actions.inputChanged(ListingForm.NEW, "ram_gb", "lots")
        val validation = validateNewListingDraft(state.value)
        assertNull(validation.draft)
        assertEquals(setOf("ram_gb"), validation.attributeErrors.keys)
        actions.showErrors(ListingForm.NEW, validation.attributeErrors)
        assertEquals("lots", state.value.newAttributes.inputs["ram_gb"])
        actions.inputChanged(ListingForm.NEW, "ram_gb", "16")
        assertTrue(state.value.newAttributes.errors.isEmpty())
    }

    @Test
    fun blankOptionalFieldsAreOmittedAndBaseErrorsStillWin() {
        actions.inputChanged(ListingForm.NEW, "brand", "   ")
        assertEquals(ListingAttributes.EMPTY, validateNewListingDraft(state.value).draft?.attributes)
        val noTitle = validateNewListingDraft(state.value.copy(newTitle = ""))
        assertEquals("Title must be at least 3 characters", noTitle.errorMessage)
    }

    @Test
    fun editDraftCarriesPreservedValuesIntoTheSubmittedAttributes() {
        val listing = attributeListing(attributes = laptopAttributes.withPreserved(mapOf("future" to "\"x\"")))
        state.value = state.value.copy(
            editingListing = listing,
            editTitle = "Laptop",
            editDescription = "d",
            editPriceCents = "100",
            editCategory = ListingCategory.ELECTRONICS,
            editAttributes = ListingAttributeDraft(
                inputs = mapOf("brand" to "Framework", "device_type" to "laptop"),
                preserved = listing.attributes.preserved
            )
        )
        val submitted = validateEditListingDraft(state.value).draft?.attributes
        assertEquals(mapOf("future" to "\"x\""), submitted?.preserved)
        assertEquals(setOf("brand", "device_type"), submitted?.values?.keys)
    }

    @Test
    fun suggestionsAndDisplayAreStable() {
        assertEquals(
            listOf("Dell"),
            ListingAttributeSuggestions.suggestionsFor(ListingCategory.ELECTRONICS, "brand", "de")
        )
        assertTrue(
            ListingAttributeSuggestions.suggestionsFor(ListingCategory.ELECTRONICS, "device_type", "de").isEmpty()
        )
        val lines = ListingAttributeDisplay.linesFor(
            ListingCategory.HOUSING,
            housingAttributes
        ).map { it.label to it.value }
        assertEquals("Monthly rent" to "\$1,250.50/mo", lines.first())
        assertTrue(ListingAttributeDisplay.linesFor(ListingCategory.OTHER, ListingAttributes.EMPTY).isEmpty())
    }
}
