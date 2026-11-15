package com.jdrms.bulletin.domain.messages.presentation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme
import com.jdrms.bulletin.domain.messages.application.IncomingMessageAlert
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class InAppMessageBannerDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var alert by mutableStateOf<IncomingMessageAlert?>(bannerAlert("m1", content = "Still available?"))
    private val opened = mutableListOf<MessageId>()

    private fun show(restoration: StateRestorationTester? = null) {
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            BulletinTheme {
                InAppMessageBannerHost(
                    alert = alert,
                    onOpen = {
                        opened += it.messageId
                        alert = null
                    },
                    onDismiss = { id -> if (alert?.messageId == id) alert = null }
                )
            }
        }
        if (restoration != null) restoration.setContent(content) else compose.setContent(content)
    }

    @Test
    fun autoDismissesAfterAboutFourSeconds() {
        compose.mainClock.autoAdvance = false
        show()
        compose.mainClock.advanceTimeBy(3_500)
        compose.onNodeWithText("Still available?").assertExists()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onAllNodesWithText("Still available?").assertCountEquals(0)
    }

    @Test
    fun swipeUpDismisses() {
        show()
        compose.onNodeWithText("Still available?").performTouchInput {
            swipe(center, Offset(center.x, center.y - 400f))
        }
        compose.waitForIdle()
        compose.onAllNodesWithText("Still available?").assertCountEquals(0)
    }

    @Test
    fun tapOpensTheConversationOnce() {
        show()
        compose.onNodeWithText("Still available?").performClick()
        compose.waitForIdle()
        assertEquals(listOf(MessageId("m1")), opened)
        compose.onAllNodesWithText("Still available?").assertCountEquals(0)
    }

    @Test
    fun isAPoliteLiveRegion() {
        show()
        compose.onNodeWithText("Still available?").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        )
    }

    @Test
    fun rotationKeepsOneVisibleBannerAndDoesNotRestoreADismissedOne() {
        val restoration = StateRestorationTester(compose)
        show(restoration)
        restoration.emulateSavedInstanceStateRestore()
        compose.onAllNodesWithText("Still available?").assertCountEquals(1)
        compose.runOnIdle { alert = null }
        restoration.emulateSavedInstanceStateRestore()
        compose.onAllNodesWithText("Still available?").assertCountEquals(0)
    }
}
