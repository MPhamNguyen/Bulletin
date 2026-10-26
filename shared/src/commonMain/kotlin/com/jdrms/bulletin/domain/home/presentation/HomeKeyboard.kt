package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.offset
import com.jdrms.bulletin.core.designsystem.LocalNavbarHeight

/** Uses Navbar's existing IME signal; its parent already reserves Navbar space when the IME is closed. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeKeyboardViewport(
    modifier: Modifier = Modifier,
    photoPlaybackPaused: Boolean = false,
    content: @Composable BoxScope.(Boolean) -> Unit
) {
    val keyboardVisible = WindowInsets.isImeVisible
    val ime = WindowInsets.ime
    val navbar = LocalNavbarHeight.current
    Box(modifier.fillMaxSize().homeImeNavbarSpace(ime, navbar, keyboardVisible).imePadding()) {
        content(photoPlaybackPaused || keyboardVisible)
    }
}

@Composable
internal fun HomeTrayKeyboard(trayOpen: Boolean) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    var hadTray by remember { mutableStateOf(false) }
    LaunchedEffect(trayOpen) {
        if (trayOpen) {
            hadTray = true
            // The tray and its focus target are attached by the settle/focus effects first.
            repeat(2) { withFrameNanos { } }
            keyboard?.show()
        } else if (hadTray) {
            focus.clearFocus(force = true)
            keyboard?.hide()
            hadTray = false
        }
    }
    DisposableEffect(keyboard) {
        onDispose { if (hadTray) keyboard?.hide() }
    }
}

/** Extend into the parent's reserved Navbar area only while the IME already covers that much space. */
private fun Modifier.homeImeNavbarSpace(ime: WindowInsets, navbar: Dp, keyboardVisible: Boolean): Modifier =
    layout { measurable, constraints ->
        val reclaim = if (keyboardVisible) navbar.roundToPx().coerceIn(0, ime.getBottom(this)) else 0
        val child = measurable.measure(constraints.offset(vertical = reclaim))
        layout(child.width, (child.height - reclaim).coerceAtLeast(0)) { child.placeRelative(0, 0) }
    }
