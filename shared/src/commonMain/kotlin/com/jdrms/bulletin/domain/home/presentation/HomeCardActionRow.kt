package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bulletin.shared.generated.resources.*
import com.jdrms.bulletin.core.designsystem.BulletinFeedCardDefaults
import com.jdrms.bulletin.domain.home.domain.model.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal data class ActionButtonUi(
    val action: CardAction,
    val label: StringResource,
    val icon: ImageVector,
    val enabled: Boolean,
    val disabledHint: StringResource?
)

internal fun AvailableActions.toButtonUi(): List<ActionButtonUi> = CardAction.entries.map { action ->
    val (label, icon) = when (action) {
        CardAction.Pass -> Res.string.home_action_pass to Icons.Filled.Close
        CardAction.Message -> Res.string.home_button_message to Icons.AutoMirrored.Outlined.Message
        CardAction.Save -> Res.string.home_action_save to Icons.Outlined.BookmarkBorder
    }
    val hint = when ((this[action] as? ActionAvailability.Unavailable)?.reason) {
        UnavailableReason.GuestUser -> Res.string.home_signed_out
        UnavailableReason.SavingNotConfigured -> Res.string.home_save_not_configured
        UnavailableReason.MessagingNotConfigured -> Res.string.home_message_not_configured
        else -> null
    }
    ActionButtonUi(action, label, icon, allows(action), hint)
}

internal fun homeActionRowAlpha(progress: Float): Float = (1f - progress * 2.5f).coerceIn(0f, 1f)

@Composable
internal fun HomeCardActionRow(
    buttons: List<ActionButtonUi>,
    onAction: (CardAction) -> Unit,
    progress: Float,
    hidden: Boolean,
    modifier: Modifier = Modifier,
    reservedHeight: Dp = 48.dp,
    actionModifier: @Composable (CardAction, Boolean) -> Modifier = { _, _ -> Modifier }
) {
    // Transparent disabled buttons still intercept touches on the composer underneath.
    if (homeActionRowAlpha(progress) == 0f) {
        Spacer(modifier.fillMaxWidth().height(reservedHeight))
        return
    }
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min).graphicsLayer { alpha = homeActionRowAlpha(progress) }
            .then(if (hidden || homeActionRowAlpha(progress) == 0f) Modifier.clearAndSetSemantics { } else Modifier),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        buttons.forEach { button ->
            val enabled = button.enabled && !hidden && progress == 0f
            HomeActionButton(
                button.copy(enabled = enabled),
                { onAction(button.action) },
                Modifier.weight(1f).fillMaxHeight().then(actionModifier(button.action, enabled))
            )
        }
    }
}

@Composable
private fun HomeActionButton(button: ActionButtonUi, onClick: () -> Unit, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val frame = homeActionFrame(modifier, interaction, button.disabledHint)
    if (button.action == CardAction.Message) {
        Button(
            onClick,
            frame,
            enabled = button.enabled,
            shape = BulletinFeedCardDefaults.actionShape,
            colors = homeActionColors(true),
            contentPadding = PaddingValues(8.dp),
            interactionSource = interaction
        ) {
            HomeActionContent(button)
        }
    } else {
        OutlinedButton(
            onClick,
            frame,
            enabled = button.enabled,
            shape = BulletinFeedCardDefaults.actionShape,
            colors = homeActionColors(false),
            contentPadding = PaddingValues(8.dp),
            interactionSource = interaction,
            border = BorderStroke(1.5.dp, BulletinFeedCardDefaults.actionOutline)
        ) {
            HomeActionContent(button)
        }
    }
}

@Composable
private fun homeActionFrame(
    modifier: Modifier,
    interaction: MutableInteractionSource,
    disabledHint: StringResource?
): Modifier {
    val focused by interaction.collectIsFocusedAsState()
    val hint = disabledHint?.let { stringResource(it) }
    return modifier.heightIn(min = 48.dp)
        .then(
            if (focused) {
                Modifier.border(
                    3.dp,
                    BulletinFeedCardDefaults.actionFocusRing,
                    BulletinFeedCardDefaults.actionShape
                ).border(6.dp, BulletinFeedCardDefaults.scrim, BulletinFeedCardDefaults.actionShape)
            } else {
                Modifier
            }
        )
        .semantics { if (hint != null) stateDescription = hint }
}

@Composable
private fun homeActionColors(primary: Boolean): ButtonColors {
    val container = if (primary) {
        BulletinFeedCardDefaults.actionMessageContainer
    } else {
        BulletinFeedCardDefaults.actionSurface
    }
    val content = if (primary) {
        BulletinFeedCardDefaults.actionMessageContent
    } else {
        BulletinFeedCardDefaults.actionContent
    }
    return ButtonDefaults.buttonColors(
        containerColor = container,
        contentColor = content,
        disabledContainerColor = container.copy(alpha = .65f),
        disabledContentColor = content.copy(alpha = .6f)
    )
}

@Composable
private fun RowScope.HomeActionContent(button: ActionButtonUi) {
    val tint = when (button.action) {
        CardAction.Pass -> BulletinFeedCardDefaults.actionPassTint
        CardAction.Save -> BulletinFeedCardDefaults.actionSaveTint
        CardAction.Message -> LocalContentColor.current
    }
    Icon(
        button.icon,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = tint.copy(alpha = if (button.enabled) 1f else .6f)
    )
    Spacer(Modifier.width(6.dp))
    Text(
        stringResource(button.label),
        modifier = Modifier.weight(1f, fill = false),
        style = MaterialTheme.typography.labelLarge
    )
}

internal fun HomeCardActions.perform(action: CardAction) = when (action) {
    CardAction.Pass -> passCurrentCard()
    CardAction.Message -> openTray()
    CardAction.Save -> saveCurrentCard()
}
