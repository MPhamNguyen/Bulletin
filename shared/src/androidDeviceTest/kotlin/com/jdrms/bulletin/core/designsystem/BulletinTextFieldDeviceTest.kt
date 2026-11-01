package com.jdrms.bulletin.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinTextFieldDefaults
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class BulletinTextFieldDeviceTest(private val dark: Boolean, private val enabled: Boolean) {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun usesMediumShapeAndSurfaceVariantContainer() {
        var background = Color.Unspecified
        var container = Color.Unspecified
        var density = 1f
        var shapeIsMedium = false
        compose.setContent {
            BulletinTheme(darkTheme = dark) {
                background = MaterialTheme.colorScheme.background
                container = MaterialTheme.colorScheme.surfaceVariant
                density = LocalDensity.current.density
                shapeIsMedium = BulletinTextFieldDefaults.shape == MaterialTheme.shapes.medium
                Box(
                    Modifier.fillMaxSize().background(background).padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BulletinTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().testTag("field"),
                        decoration = BulletinFieldDecoration(placeholder = { Text("Placeholder") }),
                        options = BulletinFieldOptions(enabled = enabled)
                    )
                }
            }
        }
        compose.waitForIdle()
        assertEquals(true, shapeIsMedium)
        val image = compose.onNodeWithTag("field").captureToImage()
        assertPixelColor("rounded corner", background, image.pixel(1, 1))
        assertPixelColor("container", container, image.pixel((8 * density).toInt(), image.height / 2))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "dark={0} enabled={1}")
        fun cases(): List<Array<Any>> =
            listOf(false, true).flatMap { dark -> listOf(true, false).map { enabled -> arrayOf(dark, enabled) } }
    }
}
