package com.jdrms.bulletin.domain.listings.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ListingPriceActionsTest {
    private val state = MutableStateFlow(ListingsUiState(newPriceCents = "125", editPriceCents = "250"))
    private val actions = ListingPriceActions(state)

    @Test
    fun invalidNewPriceKeepsTheAcceptedAmountAndExplainsTheError() {
        actions.changeNewPrice("-1.00")

        assertEquals("125", state.value.newPriceCents)
        assertEquals("250", state.value.editPriceCents)
        assertEquals("Amount cannot be negative.", state.value.errorMessage)
    }

    @Test
    fun invalidEditPriceKeepsTheAcceptedAmountAndExplainsTheError() {
        actions.changeEditPrice("10000000.00")

        assertEquals("250", state.value.editPriceCents)
        assertEquals("125", state.value.newPriceCents)
        assertEquals("Amount cannot exceed $9,999,999.99.", state.value.errorMessage)
    }

    @Test
    fun correctingNewPriceClearsTheValidationError() {
        actions.changeNewPrice("-1.00")
        actions.changeNewPrice("12.50")

        assertEquals("1250", state.value.newPriceCents)
        assertNull(state.value.errorMessage)
    }

    @Test
    fun correctingEditPriceClearsTheValidationErrorAndAllowsZero() {
        actions.changeEditPrice("1000000000")
        actions.changeEditPrice("0")

        assertEquals("0", state.value.editPriceCents)
        assertNull(state.value.errorMessage)
    }

    @Test
    fun clearingAnAmountKeepsItUnset() {
        actions.changeNewPrice("")
        actions.changeEditPrice("")

        assertEquals("", state.value.newPriceCents)
        assertEquals("", state.value.editPriceCents)
    }
}
