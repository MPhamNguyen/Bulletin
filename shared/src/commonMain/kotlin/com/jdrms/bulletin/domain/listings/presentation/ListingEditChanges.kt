package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.designsystem.amountToCurrencyDigits
import com.jdrms.bulletin.core.designsystem.currencyDigitsToAmount
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeKeys
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputFormatter

/** Compares the form with the persisted snapshot loaded when editing started. */
internal val ListingsUiState.hasEditChanges: Boolean
    get() {
        val listing = editingListing ?: return false
        if (editTitle.trim() != listing.title.trim() || editDescription.trim() != listing.description.trim() ||
            currencyDigitsToAmount(editPriceCents) != listing.price.amount
        ) {
            return true
        }
        if (editCategory != listing.category || editCondition != listing.condition ||
            editPhotos != listing.images.map { ListingPhotoDraft.Existing(it) }
        ) {
            return true
        }
        val original = ListingAttributeDraft(
            inputs = ListingAttributeInputFormatter.inputsFor(listing.attributes)
                .minus(ListingAttributeKeys.MONTHLY_RENT_USD),
            preserved = listing.attributes.preserved
        )
        val current = parseListingFormAttributes(editCategory, editAttributes, editPriceCents)
        val baseline = parseListingFormAttributes(
            listing.category,
            original,
            amountToCurrencyDigits(listing.price.amount)
        )
        return if (current.attributes != null && baseline.attributes != null) {
            current.attributes != baseline.attributes
        } else {
            editAttributes.inputs != original.inputs || editAttributes.preserved != original.preserved
        }
    }
