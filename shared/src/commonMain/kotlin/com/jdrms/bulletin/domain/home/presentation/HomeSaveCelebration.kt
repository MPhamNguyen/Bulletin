package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import bulletin.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

internal data class HomeSaveTarget(val xDp: Float, val yDp: Float)

internal fun homeSaveTarget(anchor: Rect?, center: Offset?, density: Float): HomeSaveTarget? {
    if (anchor == null || center == null || anchor.isEmpty) return null
    return HomeSaveTarget((anchor.center.x - center.x) / density, (anchor.center.y - center.y) / density)
}

/** A confirmed result may arrive before or after the optimistic flight. Neither can hold the exit callback. */
internal fun homeSaveCelebrationReady(effect: HomeEffect.SaveConfirmed, interaction: HomeInteractionState): Boolean =
    interaction.departure?.let { it.kind == HomeDepartureKind.SAVE && it.listingId == effect.listingId } != true

@Composable
internal fun HomeConfirmedSaveCelebration(
    state: HomeInteractionState,
    actions: HomeCardActions,
    anchor: Rect?,
    durationScale: Float,
    onCelebrate: (Float) -> Unit
) {
    val celebrate by rememberUpdatedState(onCelebrate)
    val announcements = LocalHomeAnnouncements.current
    val saved = stringResource(Res.string.home_saved)
    val confirmed = state.pendingEffects.filterIsInstance<HomeEffect.SaveConfirmed>()
    LaunchedEffect(confirmed, state.departure?.id, anchor == null, durationScale) {
        confirmed.forEach { effect ->
            announcements.once("save:${effect.id}", saved)
            if (anchor == null || durationScale == 0f) {
                actions.onEffectConsumed(effect.id)
            } else if (homeSaveCelebrationReady(effect, state)) {
                celebrate(durationScale)
                actions.onEffectConsumed(effect.id)
            }
        }
    }
}
