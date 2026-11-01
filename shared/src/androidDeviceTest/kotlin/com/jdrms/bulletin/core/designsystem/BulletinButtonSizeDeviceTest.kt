package com.jdrms.bulletin.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme
import org.junit.Rule
import org.junit.Test

class BulletinButtonSizeDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun regularButtonIsAtLeastControlHeight() =
        assertMinimumHeight(BulletinButtonSize.Regular, BulletinSpacing.control)

    @Test fun largeButtonIsAtLeastLargeControlHeight() =
        assertMinimumHeight(BulletinButtonSize.Large, BulletinSpacing.controlLarge)

    private fun assertMinimumHeight(size: BulletinButtonSize, minimum: Dp) {
        compose.setContent {
            BulletinTheme(darkTheme = false) {
                BulletinButton({}, Modifier.testTag("button"), size = size) { Text("Action") }
            }
        }
        compose.onNodeWithTag("button").assertHeightIsAtLeast(minimum)
    }
}
