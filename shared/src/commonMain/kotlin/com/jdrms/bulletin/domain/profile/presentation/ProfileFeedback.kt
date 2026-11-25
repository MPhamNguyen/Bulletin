package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.common.UserMessenger
import com.jdrms.bulletin.core.designsystem.BulletinToastSnackbarHost
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/** Collect profile feedback independently of destination changes. */
@Composable
internal fun ProfileFeedback(messenger: UserMessenger, modifier: Modifier = Modifier) {
    val host = remember { SnackbarHostState() }
    LaunchedEffect(messenger) {
        messenger.messages.collect { host.showSnackbar(it) }
    }
    BulletinToastSnackbarHost(
        host,
        // The app Scaffold already places profile content below the status bar.
        modifier.consumeWindowInsets(WindowInsets.statusBars.only(WindowInsetsSides.Top))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(
                top = BulletinSpacing.xl,
                start = BulletinSpacing.l,
                end = BulletinSpacing.l
            )
    )
}
