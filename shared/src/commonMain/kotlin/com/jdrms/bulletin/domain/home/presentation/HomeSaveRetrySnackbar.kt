package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import bulletin.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private data class HomeRetrySnackbarVisuals(
    val effectId: Long,
    override val message: String,
    override val actionLabel: String,
    override val withDismissAction: Boolean = true,
    override val duration: SnackbarDuration = SnackbarDuration.Long
) : SnackbarVisuals

@Composable
internal fun HomeSaveRetryNavigation(
    isHome: Boolean,
    state: HomeUiState,
    presentation: HomeSaveRetryPresentation,
    actions: HomeCardActions
) {
    LaunchedEffect(isHome, state.deckGeneration, state.interaction.pendingEffects) {
        presentation.synchronize(isHome, state, actions)
    }
}

@Composable
internal fun HomeSaveRetrySnackbar(state: HomeUiState, actions: HomeCardActions, modifier: Modifier = Modifier) {
    val local = LocalHomeSaveRetryPresentation.current
    val fallback = rememberSaveable(saver = HomeSaveRetryPresentation.saver) { HomeSaveRetryPresentation() }
    val presentation = local ?: fallback
    val host = remember { SnackbarHostState() }
    HomeSaveRetryNavigation(true, state, presentation, actions)
    HomeRetrySnackbarDelivery(presentation, host, actions)
    SnackbarHost(host, modifier)
}

@Composable
private fun HomeRetrySnackbarDelivery(
    presentation: HomeSaveRetryPresentation,
    host: SnackbarHostState,
    actions: HomeCardActions
) {
    val offer = presentation.offers.firstOrNull()
    val message = stringResource(Res.string.home_save_failed)
    val retry = stringResource(Res.string.home_retry)
    LaunchedEffect(offer?.effect?.id) {
        if (offer == null) return@LaunchedEffect
        val result = host.showSnackbar(HomeRetrySnackbarVisuals(offer.effect.id, message, retry))
        if (presentation.take(offer.effect.id) != null) {
            if (result == SnackbarResult.ActionPerformed) {
                actions.retrySave(offer.effect.listingId)
            } else {
                actions.dismissSaveRetry(offer.effect.listingId)
            }
        }
    }
    val visibleId = (host.currentSnackbarData?.visuals as? HomeRetrySnackbarVisuals)?.effectId
    LaunchedEffect(visibleId) {
        if (visibleId != null && presentation.shown(visibleId)) actions.onEffectConsumed(visibleId)
    }
}
