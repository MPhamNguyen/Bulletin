package com.jdrms.bulletin.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.MutableMainNavigationState
import com.jdrms.bulletin.domain.home.presentation.HomeScreen
import com.jdrms.bulletin.domain.listings.presentation.ListingsScreen
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceScreen
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceScreenActions
import com.jdrms.bulletin.domain.messages.presentation.MessagesScreen

@Composable
internal fun MainAppContent(
    container: AppContainer,
    navigation: MutableMainNavigationState,
    models: MainScaffoldModels,
    profilePresentation: ProfilePresentation,
    profileTabCenter: Offset,
    onListingSaved: () -> Unit,
    onContentNoLongerScrollable: () -> Unit
) {
    when (navigation.currentDestination) {
        AppDestination.HOME -> {
            val conversationActions = ListingConversationActions(container, navigation, models.messages)
            val marketplaceState by models.marketplace.uiState.collectAsState()
            var failedSaveItem by remember { mutableStateOf<MarketplaceItemId?>(null) }
            HomeScreen(
                viewModel = models.home,
                onSendQuickMessage = { item, content ->
                    val sellerId = item.sellerId
                    val sellerName = item.sellerName
                    sellerId != null && sellerName != null && conversationActions.sendQuickMessage(
                        item.id.value,
                        sellerId,
                        sellerName,
                        content
                    )
                },
                onSaveListing = { item ->
                    onListingSaved()
                    val itemId = MarketplaceItemId(item.id.value)
                    failedSaveItem = itemId
                    models.marketplace.bookmarkListing(itemId)
                },
                saveErrorMessage = marketplaceState.bookmarkErrorMessage,
                onRetrySave = { failedSaveItem?.let { models.marketplace.bookmarkListing(it) } },
                profileTargetPosition = profileTabCenter
            )
        }
        AppDestination.MARKETPLACE -> {
            val conversationActions = ListingConversationActions(container, navigation, models.messages)
            MarketplaceScreen(
                viewModel = models.marketplace,
                sellerProfileViewModel = models.sellerProfile,
                userReportViewModel = models.marketplaceUserReport,
                listingReportViewModel = models.listingReport,
                userBlockViewModel = models.marketplaceUserBlock,
                onContentNoLongerScrollable = onContentNoLongerScrollable,
                actions = MarketplaceScreenActions(
                    onMessageSeller = conversationActions::messageSeller,
                    hasExistingConversation = conversationActions::hasExistingConversation,
                    onSeeChat = conversationActions::seeChat
                )
            )
        }
        AppDestination.LISTINGS -> ListingsScreen(models.listings)
        AppDestination.MESSAGES -> MessagesScreen(models.messages)
        AppDestination.PROFILE -> ProfileDestinationContent(
            container,
            navigation,
            models.listings,
            profilePresentation,
            models.messages
        )
    }
}
