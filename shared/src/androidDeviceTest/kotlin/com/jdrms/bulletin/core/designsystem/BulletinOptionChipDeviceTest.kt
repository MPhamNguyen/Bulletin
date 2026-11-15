package com.jdrms.bulletin.core.designsystem

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@RunWith(Parameterized::class)
class BulletinOptionChipDeviceTest(private val dark: Boolean) {
    private var density = 1f

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun statesHaveDistinctSurfacesEqualHeightsAndNativeDisabledSemantics() {
        compose.activityRule.scenario.onActivity { it.actionBar?.hide() }
        var disabledClicks = 0
        compose.setContent {
            BulletinTheme(darkTheme = dark) {
                density = LocalDensity.current.density
                Column(
                    Modifier.background(MaterialTheme.colorScheme.background),
                    verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
                ) {
                    BulletinOptionChip("16 GB", false, {}, Modifier.testTag("available"))
                    BulletinOptionChip("16 GB", true, {}, Modifier.testTag("selected"))
                    BulletinOptionChip("16 GB", false, { disabledClicks++ }, Modifier.testTag("disabled"), false)
                }
            }
        }
        compose.onNodeWithTag("available").assertIsEnabled()
        compose.onNodeWithTag("selected").assertIsSelected()
        compose.onNodeWithTag("disabled").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, disabledClicks) }
        val surfaces = listOf("available", "selected", "disabled").map(::sampleSurface)
        assertNotEquals(surfaces[0], surfaces[1])
        assertNotEquals(surfaces[0], surfaces[2])
        assertNotEquals(surfaces[1], surfaces[2])
        assertEquals(
            compose.onNodeWithTag("available").fetchSemanticsNode().boundsInRoot.width,
            compose.onNodeWithTag("disabled").fetchSemanticsNode().boundsInRoot.width
        )
    }

    private fun sampleSurface(tag: String): Color {
        val node = compose.onNodeWithTag(tag).assertHeightIsEqualTo(BulletinSpacing.control)
        val image = node.captureToImage()
        val file = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "chip-$dark-$tag.png"
        )
        file.outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        return image.pixel((6 * density).toInt(), image.height / 2)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "dark={0}")
        fun themes(): List<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
    }
}
