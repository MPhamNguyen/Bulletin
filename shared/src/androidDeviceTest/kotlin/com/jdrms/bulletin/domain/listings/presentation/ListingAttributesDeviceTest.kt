package com.jdrms.bulletin.domain.listings.presentation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ListingAttributesDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun lightThemeCategoryWarningKeepsOrDiscardsVisibleInputs() = exercise(dark = false)

    @Test fun darkThemeCategoryWarningKeepsOrDiscardsVisibleInputs() = exercise(dark = true)

    private fun exercise(dark: Boolean) {
        val state = MutableStateFlow(ListingsUiState(newCategory = ListingCategory.ELECTRONICS))
        val actions = ListingAttributeDraftActions(state)
        compose.setContent {
            val current by state.collectAsState()
            BulletinTheme(darkTheme = dark) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Button(onClick = { actions.selectCategory(ListingForm.NEW, ListingCategory.CLOTHING) }) {
                        Text("Switch to clothing")
                    }
                    ListingAttributeFields(current.newCategory, current.newAttributes, { key, value ->
                        actions.inputChanged(ListingForm.NEW, key, value)
                    })
                }
                current.pendingCategoryChange?.let {
                    ListingCategoryChangeDialog(it, actions::confirmCategoryChange, actions::cancelCategoryChange)
                }
            }
        }
        compose.onNodeWithText("Brand").performTextInput("Framework")
        compose.onNodeWithText("Laptop").performScrollTo().performClick()
        compose.onNodeWithText("RAM (GB)").performScrollTo().performTextInput("16")
        compose.onNodeWithText("Switch to clothing").performScrollTo().performClick()
        compose.onNodeWithText("Change category?").assertIsDisplayed()
        compose.onNodeWithText("Keep category").performClick()
        compose.onNodeWithText("RAM (GB)").assertTextContains("16")
        compose.onNodeWithText("Switch to clothing").performScrollTo().performClick()
        compose.onNodeWithText("Change category", substring = false).performClick()
        compose.onNodeWithText("RAM (GB)").assertDoesNotExist()
        compose.onNodeWithText("Brand").assertTextContains("Framework")
        compose.runOnIdle { assertEquals(ListingCategory.CLOTHING, state.value.newCategory) }
    }
}
