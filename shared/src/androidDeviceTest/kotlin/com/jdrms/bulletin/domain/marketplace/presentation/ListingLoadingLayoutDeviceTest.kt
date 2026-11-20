package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class ListingLoadingLayoutDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun previewSellerKeepsNameAndCardHeightWhileDetailsLoad() {
        var loading by mutableStateOf(true)
        var clicks = 0
        compose.setContent {
            BulletinTheme {
                Column(Modifier.width(360.dp).testTag("seller")) {
                    SellerInfoCard(
                        seller = if (loading) null else loadedSeller(),
                        previewName = "Sam",
                        isLoading = loading,
                        onClick = { clicks++ }
                    )
                }
            }
        }
        compose.onNodeWithText("Sam", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("School and major").assertDoesNotExist()
        val card = compose.onNodeWithTag("seller")
        val before = card.fetchSemanticsNode().boundsInRoot
        card.performTouchInput { click(center) }
        compose.runOnIdle {
            assertEquals(0, clicks)
            loading = false
        }
        compose.onNodeWithText("Physics").assertExists()
        assertEquals(before.height, card.fetchSemanticsNode().boundsInRoot.height)
        compose.onNodeWithText("Sam").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun closeRemainsVisibleAndInteractiveWhileLoading() {
        var loading by mutableStateOf(true)
        var closes = 0
        compose.setContent {
            BulletinTheme {
                Column(Modifier.width(300.dp).testTag("actions")) {
                    ListingDetailActions(
                        false,
                        null,
                        ListingDetailActionCallbacks({}, { closes++ }, {}),
                        false,
                        false,
                        loading
                    )
                }
            }
        }
        compose.onNodeWithText("Close").performClick()
        compose.runOnIdle {
            assertEquals(1, closes)
            loading = false
        }
        compose.onNodeWithText("Close").performClick()
        compose.runOnIdle { assertEquals(2, closes) }
    }

    @Test fun messageControlsAppearBeforeConversationLookupCompletes() {
        val lookup = kotlinx.coroutines.CompletableDeferred<Boolean>()
        compose.setContent {
            BulletinTheme {
                com.jdrms.bulletin.core.designsystem.MessageSellerQuickAction(
                    actions = com.jdrms.bulletin.core.designsystem.MessageSellerQuickActionActions(
                        conversationKey = "listing:seller",
                        hasExistingConversation = { lookup.await() },
                        onSend = { false },
                        onSent = {},
                        onSeeChat = {}
                    )
                )
            }
        }
        compose.onNodeWithText("Message Seller").assertExists()
        compose.onNodeWithText("Hi, is this available?").assertExists()
        compose.onNodeWithText("Send").assertExists()
        compose.onNodeWithText("Checking...").assertDoesNotExist()
        compose.runOnIdle { lookup.complete(true) }
        compose.waitForIdle()
        compose.onNodeWithText("See Chat").assertExists()
    }

    private fun loadedSeller() = SellerInfoCardState(
        name = "Sam",
        school = "Campus",
        avatarUrl = null,
        reputationScore = 4.5,
        major = "Physics",
        reviewCount = 12,
        isVerified = false
    )
}
