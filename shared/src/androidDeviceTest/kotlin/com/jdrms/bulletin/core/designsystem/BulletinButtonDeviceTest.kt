package com.jdrms.bulletin.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

/** Samples rendered colors and corners; no golden screenshot comparison. */
@RunWith(Parameterized::class)
class BulletinButtonDeviceTest(
    private val variant: BulletinButtonVariant,
    private val dark: Boolean,
    private val enabled: Boolean
) {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun rendersTokenColorsShapeAndMinimumHeight() {
        var background = Color.Unspecified
        var container = Color.Unspecified
        var density = 1f
        var shapeIsMedium = false
        compose.setContent {
            BulletinTheme(darkTheme = dark) {
                val colors = BulletinButtonDefaults.colors(variant)
                background = MaterialTheme.colorScheme.background
                container = if (enabled) colors.containerColor else colors.disabledContainerColor
                density = LocalDensity.current.density
                shapeIsMedium = BulletinButtonDefaults.shape == MaterialTheme.shapes.medium
                Box(
                    Modifier.fillMaxSize().background(background).padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BulletinButton({}, Modifier.testTag("button"), enabled, variant) { Text("Action") }
                }
            }
        }
        compose.waitForIdle()
        assertEquals(true, shapeIsMedium)
        val node = compose.onNodeWithTag("button")
        node.assertHeightIsAtLeast(BulletinSpacing.control)
        val image = node.captureToImage()
        assertPixelColor("container", container.over(background), image.pixel((8 * density).toInt(), image.height / 2))
        if (variant != BulletinButtonVariant.Text && variant != BulletinButtonVariant.DestructiveText) {
            assertPixelColor("rounded corner", background, image.pixel(1, 1))
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} dark={1} enabled={2}")
        fun cases(): List<Array<Any>> = BulletinButtonVariant.entries.flatMap { variant ->
            listOf(false, true).flatMap { dark ->
                listOf(true, false).map { enabled -> arrayOf(variant, dark, enabled) }
            }
        }
    }
}
