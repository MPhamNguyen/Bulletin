package com.jdrms.bulletin.domain.listings.presentation

import android.app.DatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDate
import java.util.Calendar

@Composable
internal actual fun rememberListingDatePicker(
    initial: ListingAttributeDate?,
    minYear: Int,
    maxYear: Int,
    onDateSelected: (ListingAttributeDate) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val currentInitial = rememberUpdatedState(initial)
    val currentOnSelected = rememberUpdatedState(onDateSelected)
    return {
        val today = Calendar.getInstance()
        val start = currentInitial.value
        val dialog = DatePickerDialog(
            context,
            { _, year, month, day -> currentOnSelected.value(ListingAttributeDate(year, month + 1, day)) },
            start?.year ?: today.get(Calendar.YEAR).coerceIn(minYear, maxYear),
            start?.let { it.month - 1 } ?: today.get(Calendar.MONTH),
            start?.day ?: today.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.minDate = dayInMillis(minYear, Calendar.JANUARY, 1)
        dialog.datePicker.maxDate = dayInMillis(maxYear, Calendar.DECEMBER, 31)
        dialog.show()
    }
}

private fun dayInMillis(year: Int, month: Int, day: Int): Long =
    Calendar.getInstance().apply { clear(); set(year, month, day) }.timeInMillis
