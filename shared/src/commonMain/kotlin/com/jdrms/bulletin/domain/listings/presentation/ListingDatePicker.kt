package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.runtime.Composable
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDate

/** Returns an action that opens the platform calendar and reports the chosen date. */
@Composable
internal expect fun rememberListingDatePicker(
    initial: ListingAttributeDate?,
    minYear: Int,
    maxYear: Int,
    onDateSelected: (ListingAttributeDate) -> Unit
): () -> Unit
