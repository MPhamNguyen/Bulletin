package com.jdrms.bulletin.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ProfileTabAnchorDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun profileHookIsUniqueAndDoesNotChangeNavigation() {
        val anchor = ProfileTabAnchor()
        var selected = AppDestination.HOME
        compose.setContent {
            BulletinTheme {
                Navbar(
                    AppDestination.HOME,
                    { true },
                    { selected = it },
                    {},
                    profileIconModifier = anchor.modifier.testTag("profile-anchor")
                )
            }
        }
        compose.onNodeWithTag("profile-anchor", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.runOnIdle {
            assertEquals(AppDestination.PROFILE, selected)
            assertNotNull(anchor.bounds(true))
            assertNull(anchor.bounds(false))
        }
    }

    @Test
    fun profileCenterUpdatesAfterLayoutResize() {
        val anchor = ProfileTabAnchor()
        val width = mutableStateOf(390.dp)
        compose.setContent {
            BulletinTheme {
                Box(Modifier.width(width.value)) {
                    Navbar(AppDestination.HOME, { true }, {}, {}, profileIconModifier = anchor.modifier)
                }
            }
        }
        var oldX = 0f
        compose.runOnIdle {
            oldX = assertNotNull(anchor.bounds(true)).center.x
            width.value = 300.dp
        }
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(assertNotNull(anchor.bounds(true)).center.x < oldX) }
    }
}
