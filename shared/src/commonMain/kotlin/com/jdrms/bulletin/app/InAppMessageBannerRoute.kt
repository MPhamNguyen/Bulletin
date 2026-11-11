package com.jdrms.bulletin.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.domain.messages.presentation.InAppMessageBannerHost

/** Connects the banner host to the app: reports screen eligibility and opens tapped conversations. */
@Composable
internal fun InAppMessageBannerRoute(container: AppContainer, isEligible: Boolean, modifier: Modifier = Modifier) {
    val banner = container.messages.inAppBanner
    LaunchedEffect(banner, isEligible) { banner.setEligible(isEligible) }
    DisposableEffect(banner) { onDispose { banner.setEligible(false) } }
    val alert by banner.visible.collectAsState()
    InAppMessageBannerHost(
        alert = alert,
        onOpen = {
            container.openConversationFromNotification(it.conversationId.value, it.recipientId.value)
            banner.dismiss(it.messageId)
        },
        onDismiss = banner::dismiss,
        modifier = modifier
    )
}
