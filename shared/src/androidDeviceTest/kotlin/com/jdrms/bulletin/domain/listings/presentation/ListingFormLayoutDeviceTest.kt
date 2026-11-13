package com.jdrms.bulletin.domain.listings.presentation

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Exercises the real details content above its fixed footer at narrow widths and larger text sizes. */
@RunWith(Parameterized::class)
class ListingFormLayoutDeviceTest(private val width: Int, private val fontScale: Float) {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun optionsWrapAndDisabledCapacitiesCannotChangeTheDraft() {
        val state = MutableStateFlow(ListingsUiState(newCategory = ListingCategory.ELECTRONICS))
        val actions = ListingAttributeDraftActions(state)
        renderForm(state, actions)
        savePreview("top")
        compose.onNodeWithText("Continue").assertIsDisplayed()
        compose.onNodeWithText("Apple").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("4 GB").performScrollTo().assertIsNotEnabled()
            .assertHeightIsEqualTo(BulletinSpacing.control).performTouchInput { click() }
        compose.runOnIdle { assertTrue("ram_gb" !in state.value.newAttributes.inputs) }
        compose.onNodeWithText("Select a device type to choose RAM.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Other").performScrollTo().assertIsDisplayed()
        val form = compose.onNodeWithTag("form").fetchSemanticsNode().boundsInRoot
        listOf("Laptop", "Desktop", "Tablet", "Phone", "Monitor", "Game console", "Accessory", "Other").forEach {
            val bounds = compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.left >= form.left && bounds.right <= form.right, "$it fits the form width")
        }
        compose.onNodeWithText("Laptop").performScrollTo().performClick()
        compose.onNodeWithText("16 GB").performScrollTo().assertIsEnabled().performClick().assertIsSelected()
        compose.runOnIdle { assertEquals("16", state.value.newAttributes.inputs["ram_gb"]) }
        savePreview("capacity")
        compose.onNodeWithText("Game console").performScrollTo().performClick()
        compose.onNodeWithText("16 GB").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("2 TB+").performScrollTo().assertIsEnabled().assertIsDisplayed()
        compose.onNodeWithText("Continue").assertIsDisplayed()
    }

    private fun renderForm(state: MutableStateFlow<ListingsUiState>, actions: ListingAttributeDraftActions) {
        compose.activityRule.scenario.onActivity { it.actionBar?.hide() }
        compose.setContent {
            val current by state.collectAsState()
            BulletinTheme(darkTheme = true) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    Column(
                        Modifier.width(width.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface)
                            .testTag("form")
                    ) {
                        CreateListingHeader(CreateListingStep.DETAILS, {})
                        Column(
                            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)
                        ) {
                            CreateListingDetailsStep(
                                current,
                                CreateListingDetailsActions({}, { key, value ->
                                    actions.inputChanged(ListingForm.NEW, key, value)
                                })
                            )
                        }
                        CreateListingFooter(CreateListingFooterState(CreateListingStep.DETAILS, true, false), {}, {})
                    }
                }
            }
        }
    }

    private fun savePreview(region: String) {
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        val file = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "listing-form-$width-$fontScale-$region.png"
        )
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "width={0} fontScale={1}")
        fun sizes(): List<Array<Any>> = listOf(arrayOf(320, 1f), arrayOf(360, 1.3f))
    }
}
