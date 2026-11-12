package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.app.integration.ListingMarketplaceReviewRequestCreator
import com.jdrms.bulletin.app.theme.InMemoryThemePreferenceStore
import com.jdrms.bulletin.app.theme.ThemePreferenceStores
import com.jdrms.bulletin.app.theme.ThemeViewModel
import com.jdrms.bulletin.core.common.FlowUserMessenger
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.network.SupabaseAuthStorage
import com.jdrms.bulletin.core.network.SupabaseConfig
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreferenceStore
import com.jdrms.bulletin.domain.marketplace.application.ReportedListingVisibilityPreferenceStore
import com.jdrms.bulletin.domain.marketplace.infrastructure.InMemoryMarketplaceLayoutPreferenceStore
import com.jdrms.bulletin.domain.marketplace.infrastructure.InMemoryReportedListingVisibilityPreferenceStore
import com.jdrms.bulletin.domain.messages.application.IncomingMessageNotifier
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.presentation.InboxNotificationCoordinator
import com.jdrms.bulletin.domain.messages.presentation.NotificationConversationRequest
import com.jdrms.bulletin.domain.profile.infrastructure.repository.ExternalAuthBrowser
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AppContainer(
    supabaseConfig: SupabaseConfig = SupabaseConfig(),
    isInspectionMode: Boolean = false,
    allowInMemoryFallback: Boolean = false,
    private val themePreferenceStore: ThemePreferenceStores = InMemoryThemePreferenceStore(),
    marketplaceLayoutPreferenceStore: MarketplaceLayoutPreferenceStore =
        InMemoryMarketplaceLayoutPreferenceStore(),
    providedSupabaseClient: SupabaseClient? = null,
    private val externalAuth: ExternalAuthConfiguration = ExternalAuthConfiguration(),
    reportedListingVisibilityPreferenceStore: ReportedListingVisibilityPreferenceStore =
        InMemoryReportedListingVisibilityPreferenceStore()
) {
    val microsoftSignInEnabled: Boolean get() = externalAuth.microsoftSignInEnabled
    val externalSignInPending: Boolean get() = externalAuth.browser?.isResuming == true

    internal val listingChangedSignal by lazy { RefreshSignal() }
    internal val userMessenger by lazy { FlowUserMessenger() }
    private val infrastructure = InfrastructureModule(
        supabaseConfig,
        isInspectionMode,
        allowInMemoryFallback,
        providedSupabaseClient,
        externalAuth.storage,
        externalAuth.microsoftSignInEnabled
    )
    internal val profile = ProfileModule(infrastructure, externalAuth.browser)

    // The application container owns the OAuth job across Activity/composition recreation.
    internal val signInViewModel by lazy { profile.createSignInViewModel() }

    internal val listings: ListingsModule = ListingsModule(
        infrastructure,
        profile,
        { marketplace.invalidateReads() },
        ListingMarketplaceReviewRequestCreator { marketplace.reviewRequestRepository }
    )
    internal val marketplace: MarketplaceModule = MarketplaceModule(
        infrastructure,
        profile,
        listings,
        marketplaceLayoutPreferenceStore,
        reportedListingVisibilityPreferenceStore
    )
    internal val messages = MessagesModule(infrastructure, profile, listings, marketplace)
    internal val home = HomeModule(marketplace, messages)

    private var inboxNotifications: InboxNotificationCoordinator? = null

    /** True while a user is signed in; platform code uses it to time the notification permission request. */
    val isSignedIn: Flow<Boolean>
        get() = messages.inboxStateOwner.state.map { it.viewerId != null }.distinctUntilChanged()

    /**
     * Idempotent: the first call binds [notifier]; later calls are ignored.
     * Alerts only while the process is alive; there is no push delivery yet. Foreground alerts feed the in-app
     * banner and background alerts go to [notifier].
     */
    fun startMessageNotifications(notifier: IncomingMessageNotifier) {
        val coordinator = inboxNotifications ?: messages.createInboxNotifications(notifier)
            .also { inboxNotifications = it }
        messages.inAppBanner.start(coordinator.alerts)
        coordinator.start()
    }

    /** Records a notification tap. It opens once [recipientId] is the signed-in user, and is dropped otherwise. */
    fun openConversationFromNotification(conversationId: String, recipientId: String) {
        if (conversationId.isBlank() || recipientId.isBlank()) return
        messages.notificationRequests.submit(
            NotificationConversationRequest(ConversationId(conversationId), SenderId(recipientId))
        )
    }

    internal fun createThemeViewModel() = ThemeViewModel(themePreferenceStore)
}

data class ExternalAuthConfiguration(
    val microsoftSignInEnabled: Boolean = false,
    val browser: ExternalAuthBrowser? = null,
    val storage: SupabaseAuthStorage? = null
)
