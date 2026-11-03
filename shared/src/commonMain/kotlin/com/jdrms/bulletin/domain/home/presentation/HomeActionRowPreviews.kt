package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinFeedCardDefaults
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme
import com.jdrms.bulletin.domain.home.domain.model.HomeActionContext
import com.jdrms.bulletin.domain.home.domain.model.availableActions

@Composable
private fun ActionRowPreview(guest: Boolean = false, dark: Boolean = false) {
    BulletinTheme(darkTheme = dark) {
        Box(Modifier.width(366.dp).background(BulletinFeedCardDefaults.scrim).padding(BulletinSpacing.m)) {
            HomeCardActionRow(
                CardState.Idle.availableActions(HomeActionContext(true, !guest, true, true))
                    .toButtonUi(),
                {},
                0f,
                false
            )
        }
    }
}

@Preview(name = "Idle")
@Composable
internal fun HomeActionsIdlePreview() = ActionRowPreview()

@Preview(name = "Guest Save disabled")
@Composable
internal fun HomeActionsGuestPreview() = ActionRowPreview(guest = true)

@Preview(name = "Dark mode")
@Composable
internal fun HomeActionsDarkPreview() = ActionRowPreview(dark = true)

@Preview(name = "200% font", fontScale = 2f)
@Composable
internal fun HomeActionsLargeFontPreview() = ActionRowPreview()
