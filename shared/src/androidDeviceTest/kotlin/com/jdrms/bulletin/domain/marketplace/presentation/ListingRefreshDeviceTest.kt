package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceListingStatus
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePrice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class ListingRefreshDeviceTest(private val bottomSheet: Boolean) {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var state by mutableStateOf(loadedState())
    private var send: suspend (String) -> Boolean = { false }

    @Test fun refreshKeepsCustomDraftAndDisablesOnlyNewSendUntilFinished() {
        var submitted: String? = null
        send = {
            submitted = it
            false
        }
        showDetail()
        enterDraft()
        compose.runOnIdle { state = state.copy(isLoading = true) }
        compose.onNodeWithText(DRAFT).assertExists()
        compose.onNodeWithText("Listing title").assertExists()
        compose.onNodeWithText("Send").assertIsNotEnabled()
        compose.runOnIdle {
            state = state.copy(isLoading = false, listing = state.listing?.copy(price = MarketplacePrice(30.0)))
        }
        compose.onNodeWithText(DRAFT).assertExists()
        compose.onNodeWithText("Send").assertIsEnabled().performScrollTo().performClick()
        compose.runOnIdle { assertEquals(DRAFT, submitted) }
    }

    @Test fun refreshDoesNotCancelAnInFlightSend() {
        val response = CompletableDeferred<Boolean>()
        var started = false
        var completed = false
        var cancelled = false
        send = {
            started = true
            try {
                response.await().also { completed = true }
            } catch (exception: CancellationException) {
                cancelled = true
                throw exception
            }
        }
        showDetail()
        enterDraft()
        compose.onNodeWithText("Send").performScrollTo().performClick()
        compose.runOnIdle {
            assertTrue(started)
            state = state.copy(isLoading = true)
        }
        compose.onNodeWithText(DRAFT).assertExists()
        compose.runOnIdle {
            assertFalse(cancelled)
            response.complete(false)
        }
        compose.onNodeWithText("Unable to send message. Please try again.").assertExists()
        compose.runOnIdle { assertTrue(completed) }
    }

    @Test fun refreshDisablesReviewUntilFreshEligibilityArrives() {
        state = state.copy(listing = state.listing?.copy(status = MarketplaceListingStatus.SOLD, buyerId = "viewer"))
        showDetail()
        compose.onNodeWithText("Leave Review for Seller").performScrollTo().assertIsEnabled()
        compose.runOnIdle { state = state.copy(isLoading = true) }
        compose.onNodeWithText("Leave Review for Seller").assertIsNotEnabled()
        compose.runOnIdle { state = state.copy(isLoading = false, hasReview = true) }
        compose.onNodeWithText("Leave Review for Seller").assertDoesNotExist()
    }

    private fun enterDraft() {
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement(DRAFT)
    }

    private fun showDetail() {
        compose.setContent {
            BulletinTheme {
                val actions = MarketplaceDetailSheetActions(
                    onDismiss = {},
                    onRetry = {},
                    onToggleBookmark = {},
                    hasExistingConversation = { _, _ -> false },
                    onMessageSeller = { _, _, _, content -> send(content) }
                )
                if (bottomSheet) MarketplaceDetailBottomSheet(state, actions) else ListingDetailsPage(state, actions)
            }
        }
    }

    companion object {
        private const val DRAFT = "Could I pick this up after class?"

        @JvmStatic
        @Parameterized.Parameters(name = "bottomSheet={0}")
        fun cases(): List<Array<Any>> = listOf(arrayOf(false), arrayOf(true))

        private fun loadedState() = MarketplaceDetailSheetState(
            listing = Listing(
                id = MarketplaceItemId("listing"),
                sellerId = "seller",
                sellerName = "Seller",
                title = "Listing title",
                description = "Description",
                price = MarketplacePrice(20.0),
                category = MarketplaceCategory.OTHER
            ),
            isLoading = false,
            errorMessage = null,
            bookmarkErrorMessage = null,
            isBookmarked = false,
            currentUserId = "viewer",
            hasReview = false
        )
    }
}
