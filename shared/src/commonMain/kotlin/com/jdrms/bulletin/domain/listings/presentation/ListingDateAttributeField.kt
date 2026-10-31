package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.DateInput
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDefinition
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeType
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser

@Composable
internal fun ListingDateAttributeField(
    category: ListingCategory,
    definition: ListingAttributeDefinition,
    value: String,
    error: String?,
    onInputChange: (String, String) -> Unit
) {
    val type = definition.type as ListingAttributeType.Date
    val parser = remember { ListingAttributeInputParser() }
    val openCalendar = rememberListingDatePicker(
        initial = parseDateInput(value),
        minYear = type.minYear,
        maxYear = type.maxYear,
        onDateSelected = { onInputChange(definition.key, it.toString()) }
    )
    val message = error ?: dateInputError(parser, category, definition, value)
    DateInput(
        value = value,
        onValueChange = { onInputChange(definition.key, it) },
        label = ListingAttributeDisplay.fieldLabel(definition),
        onOpenCalendar = openCalendar,
        error = message,
        modifier = Modifier.fillMaxWidth()
    )
}
