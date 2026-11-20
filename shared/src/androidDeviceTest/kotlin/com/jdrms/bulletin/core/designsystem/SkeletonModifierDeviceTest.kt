package com.jdrms.bulletin.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SkeletonModifierDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun replacesDrawingAndSemanticsWithoutChangingMeasurement() {
        var loading by mutableStateOf(true)
        val contentDraws = AtomicInteger()
        compose.setContent {
            BulletinTheme {
                Box(Modifier.testTag("bounds")) {
                    Text(
                        "Private content",
                        modifier = Modifier.size(180.dp, 48.dp).skeleton(loading).drawWithContent {
                            contentDraws.incrementAndGet()
                            drawRect(Color.Red)
                            drawContent()
                        }
                    )
                }
            }
        }
        val bounds = compose.onNodeWithTag("bounds")
        val before = bounds.fetchSemanticsNode().boundsInRoot
        bounds.captureToImage()
        compose.onNodeWithText("Private content").assertDoesNotExist()
        assertEquals(0, contentDraws.get(), "Skeleton must skip the underlying draw chain")
        compose.runOnIdle { loading = false }
        compose.onNodeWithText("Private content").assertExists()
        bounds.captureToImage()
        assertTrue(contentDraws.get() > 0)
        assertEquals(before, bounds.fetchSemanticsNode().boundsInRoot)
    }
}
