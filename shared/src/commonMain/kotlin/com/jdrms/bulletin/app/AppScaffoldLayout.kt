package com.jdrms.bulletin.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.navigation.MutableMainNavigationState
import com.jdrms.bulletin.core.designsystem.BulletinExtras
import com.jdrms.bulletin.core.designsystem.LocalNavbarHeight
import com.jdrms.bulletin.domain.home.presentation.HomeSaveRetryPresentation
import com.jdrms.bulletin.domain.home.presentation.LocalHomeSaveRetryPresentation
import kotlinx.coroutines.CoroutineScope

internal class MainAppContentState(
    val container: AppContainer,
    val navigation: MutableMainNavigationState,
    val models: MainScaffoldModels,
    val profile: ProfilePresentation
)

internal class MainAppChrome(
    val behavior: NavbarScrollBehavior,
    val nestedScroll: NavbarNestedScrollConnection,
    val anchor: ProfileTabAnchor,
    val celebration: ProfileTabCelebration,
    val scope: CoroutineScope,
    val haptics: HapticFeedback
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MainAppLayout(content: MainAppContentState, chrome: MainAppChrome, retries: HomeSaveRetryPresentation) {
    val navigation = content.navigation
    val density = androidx.compose.ui.platform.LocalDensity.current
    val inset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    var navbarHeight by remember { mutableStateOf(0.dp) }
    var isConversationOpen by remember { mutableStateOf(false) }
    val keyboardVisible = WindowInsets.isImeVisible
    Box(Modifier.fillMaxSize()) {
        MainDestinationBody(
            content,
            chrome,
            retries,
            navbarHeight,
            chrome.anchor.bounds(
                navigation.showsBottomNavigation && chrome.behavior.isVisible && !isConversationOpen && !keyboardVisible
            ),
            onConversationVisibilityChanged = { isOpen ->
                isConversationOpen = isOpen
                if (isOpen) chrome.behavior.hide() else chrome.behavior.reveal()
            }
        )
        Navbar(
            currentDestination = navigation.currentDestination,
            isVisible = {
                navigation.showsBottomNavigation && !isConversationOpen && chrome.behavior.isVisible
            },
            onDestinationSelected = { navigateTo(it, navigation, content.models.listings) },
            onFocused = chrome.behavior::reveal,
            profileIconModifier = chrome.anchor.modifier.then(
                chrome.celebration.modifier(BulletinExtras.colors.success)
            ),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(100f).onSizeChanged {
                navbarHeight = with(density) { it.height.toDp() } - inset
            }
        )
    }
}

@Composable
private fun MainDestinationBody(
    content: MainAppContentState,
    chrome: MainAppChrome,
    retries: HomeSaveRetryPresentation,
    navbarHeight: Dp,
    anchor: Rect?,
    onConversationVisibilityChanged: (Boolean) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { padding ->
        CompositionLocalProvider(
            LocalNavbarHeight provides navbarHeight,
            LocalHomeSaveRetryPresentation provides retries
        ) {
            Box(mainContentModifier(padding, navbarHeight, content.navigation, chrome)) {
                MainAppContent(
                    content.container,
                    content.navigation,
                    content.models,
                    content.profile,
                    onContentNoLongerScrollable = chrome.behavior::reveal,
                    onConversationVisibilityChanged = onConversationVisibilityChanged,
                    profileAnchorInWindow = anchor,
                    onSaveCelebration = { scale -> chrome.celebration.play(chrome.scope, scale, chrome.haptics) }
                )
            }
        }
    }
}

private fun mainContentModifier(
    padding: PaddingValues,
    navbarHeight: Dp,
    navigation: MutableMainNavigationState,
    chrome: MainAppChrome
): Modifier {
    val content = Modifier.padding(padding).then(
        if (navigation.currentDestination.hidesOnScroll || !navigation.showsBottomNavigation) {
            Modifier
        } else {
            Modifier.padding(bottom = navbarHeight)
        }
    ).fillMaxSize()
    return if (navigation.currentDestination.hidesOnScroll) content.nestedScroll(chrome.nestedScroll) else content
}
