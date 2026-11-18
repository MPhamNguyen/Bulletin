package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.designsystem.amountToCurrencyDigits
import com.jdrms.bulletin.domain.listings.attributes.attributeListing
import com.jdrms.bulletin.domain.listings.attributes.housingAttributes
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeKeys
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputFormatter
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListingEditChangesTest {
    private fun state(listing: Listing = attributeListing().copy(images = listOf("photo-a", "photo-b"))) =
        ListingsUiState(
            editingListing = listing,
            editTitle = listing.title,
            editDescription = listing.description,
            editPriceCents = amountToCurrencyDigits(listing.price.amount),
            editCategory = listing.category,
            editCondition = listing.condition,
            editPhotos = listing.images.map { ListingPhotoDraft.Existing(it) },
            editAttributes = ListingAttributeDraft(
                inputs = ListingAttributeInputFormatter.inputsFor(listing.attributes)
                    .minus(ListingAttributeKeys.MONTHLY_RENT_USD),
                preserved = listing.attributes.preserved
            )
        )

    @Test
    fun unchangedAndClosedFormsHaveNoChanges() {
        assertFalse(ListingsUiState().hasEditChanges)
        assertFalse(state().hasEditChanges)
        assertFalse(
            state(attributeListing(category = ListingCategory.HOUSING, attributes = housingAttributes))
                .hasEditChanges
        )
    }

    @Test
    fun eachEditableDetailCountsAndRevertingClearsChanges() {
        val original = state()
        listOf(
            original.copy(editTitle = "New title"),
            original.copy(editDescription = "New description"),
            original.copy(editPriceCents = "1234"),
            original.copy(editCategory = ListingCategory.OTHER),
            original.copy(editCondition = ListingCondition.NEW),
            original.copy(
                editAttributes = original.editAttributes.copy(
                    inputs = original.editAttributes.inputs + ("brand" to "Dell")
                )
            )
        ).forEach { changed ->
            assertTrue(changed.hasEditChanges)
            assertFalse(
                changed.copy(
                    editTitle = original.editTitle,
                    editDescription = original.editDescription,
                    editPriceCents = original.editPriceCents,
                    editCategory = original.editCategory,
                    editCondition = original.editCondition,
                    editAttributes = original.editAttributes
                ).hasEditChanges
            )
        }
    }

    @Test
    fun photoAdditionRemovalReorderingAndCropChangesCount() {
        val original = state()
        val photos = original.editPhotos
        listOf(
            photos + ListingPhotoDraft.Pending(ListingImageUpload(byteArrayOf(1), "image/jpeg")),
            photos.dropLast(1),
            photos.reversed(),
            listOf(ListingPhotoDraft.Existing("photo-a#crop")) + photos.drop(1)
        ).forEach { changed -> assertTrue(original.copy(editPhotos = changed).hasEditChanges) }
        assertFalse(original.copy(editPhotos = photos).hasEditChanges)
    }

    @Test
    fun invalidAttributesStillCountAsEdits() {
        val original = state()
        val changed = original.copy(
            editAttributes = original.editAttributes.copy(
                inputs = original.editAttributes.inputs + ("ram_gb" to "invalid")
            )
        )
        assertTrue(changed.hasEditChanges)
    }

    @Test
    fun formattingAndInlineErrorsDoNotCountAsStoredChanges() {
        val original = state()
        assertFalse(
            original.copy(
                editTitle = " ${original.editTitle} ",
                editDescription = " ${original.editDescription} ",
                editPriceCents = "0${original.editPriceCents}",
                editAttributes = original.editAttributes.copy(
                    inputs = original.editAttributes.inputs + ("ram_gb" to "016"),
                    errors = mapOf("brand" to "Old error")
                )
            ).hasEditChanges
        )
    }
}
