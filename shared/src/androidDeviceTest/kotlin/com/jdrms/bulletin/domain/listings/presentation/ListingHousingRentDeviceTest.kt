package com.jdrms.bulletin.domain.listings.presentation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingDecimal
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ListingHousingRentDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun housingShowsOnlyOneRentInputAndItsValueSuppliesBothSavedAmounts() {
        val state = MutableStateFlow(
            ListingsUiState(
                newCategory = ListingCategory.HOUSING,
                newTitle = "Campus room",
                newDescription = "Utilities included",
                newImages = listOf(ListingImageUpload(byteArrayOf(1), "image/jpeg"))
            )
        )
        render(state)
        compose.onAllNodesWithContentDescription("Monthly rent").assertCountEquals(1)
        compose.onNodeWithText("Monthly rent (USD per month)").assertDoesNotExist()
        compose.onNodeWithContentDescription("Available date").performScrollTo().performTextInput("20260901")
        compose.onNodeWithContentDescription("Monthly rent").performScrollTo().performTextInput("125000")
        compose.runOnIdle {
            val draft = assertNotNull(validateNewListingDraft(state.value).draft)
            assertEquals(1250.0, draft.price)
            assertEquals(
                ListingAttributeValue.Decimal(ListingDecimal(125000, 2)),
                draft.attributes["monthly_rent_usd"]
            )
        }
    }

    private fun render(state: MutableStateFlow<ListingsUiState>) {
        val attributes = ListingAttributeDraftActions(state)
        compose.setContent {
            val current by state.collectAsState()
            BulletinTheme(darkTheme = true) {
                Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                    CreateListingDetailsStep(
                        current,
                        CreateListingDetailsActions({}, { key, value ->
                            attributes.inputChanged(ListingForm.NEW, key, value)
                        })
                    )
                    CreateListingPriceStep(
                        current,
                        CreateListingPriceActions(
                            { digits -> state.update { it.copy(newPriceCents = digits) } },
                            { text -> state.update { it.copy(newDescription = text) } }
                        )
                    )
                }
            }
        }
    }
}
