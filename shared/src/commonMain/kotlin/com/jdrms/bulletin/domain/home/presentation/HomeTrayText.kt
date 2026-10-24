package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.runtime.Composable
import bulletin.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun homeInteractionErrorText(error: HomeInteractionError): String = stringResource(
    when (error) {
        HomeInteractionError.BLANK_MESSAGE -> Res.string.home_blank_message
        HomeInteractionError.MESSAGE_TOO_LONG -> Res.string.home_long_message
        HomeInteractionError.NOT_SIGNED_IN -> Res.string.home_signed_out
        HomeInteractionError.LISTING_UNAVAILABLE -> Res.string.home_listing_unavailable
        HomeInteractionError.OWN_LISTING -> Res.string.home_own_listing
        HomeInteractionError.NETWORK, HomeInteractionError.UNKNOWN -> Res.string.home_message_failed
        HomeInteractionError.DECISION_STORAGE -> Res.string.home_decision_failed
    }
)

@Composable
internal fun homeQuickReplies(): List<String> = listOf(
    stringResource(Res.string.home_reply_available),
    stringResource(Res.string.home_reply_meet),
    stringResource(Res.string.home_reply_photos),
    stringResource(Res.string.home_reply_price)
)
