package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bulletin.shared.generated.resources.*
import com.jdrms.bulletin.core.designsystem.theme.BulletinElevation
import com.jdrms.bulletin.core.designsystem.theme.BulletinFeedCardDefaults
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
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
        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.l, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        buttons.forEach { button ->
            val enabled = button.enabled && !hidden && progress == 0f
            val diameter = if (button.action == CardAction.Message) 72.dp else 60.dp
            HomeActionButton(
                button.copy(enabled = enabled),
                { onAction(button.action) },
                Modifier.size(diameter).then(actionModifier(button.action, enabled))
            )
        }
    }
}

@Composable
private fun HomeActionButton(button: ActionButtonUi, onClick: () -> Unit, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val shape = BulletinFeedCardDefaults.actionShape
    val isMessage = button.action == CardAction.Message
    val background = if (button.action == CardAction.Message) {
        Brush.linearGradient(
            listOf(BulletinFeedCardDefaults.actionMessageStart, BulletinFeedCardDefaults.actionMessageEnd)
        )
    } else {
        Brush.verticalGradient(
            listOf(BulletinFeedCardDefaults.actionGlassHighlight, BulletinFeedCardDefaults.actionGlass)
        )
    }
    val outline = if (isMessage) {
        BulletinFeedCardDefaults.actionMessageOutline
    } else {
        BulletinFeedCardDefaults.actionGlassOutline
    }
    // design-system-exempt: glass action over artwork needs a custom interaction source and zero padding
    Button(
        onClick = onClick,
        modifier = Modifier.shadow(
            if (isMessage) BulletinElevation.overlay else BulletinElevation.floating,
            shape
        ).background(background, shape)
            .border(1.dp, outline, shape)
            .then(homeActionFrame(modifier, interaction, button.disabledHint, shape)),
        enabled = button.enabled,
        shape = shape,
        colors = homeActionColors(),
        contentPadding = PaddingValues(0.dp),
        interactionSource = interaction
    ) {
        HomeActionContent(button)
    }
}

@Composable
private fun homeActionFrame(
    modifier: Modifier,
    interaction: MutableInteractionSource,
    disabledHint: StringResource?,
    shape: Shape
): Modifier {
    val focused by interaction.collectIsFocusedAsState()
    val hint = disabledHint?.let { stringResource(it) }
    return modifier.heightIn(min = 40.dp)
        .then(
            if (focused) {
                Modifier.border(
                    3.dp,
                    BulletinFeedCardDefaults.actionFocusRing,
                    shape
                ).border(6.dp, BulletinFeedCardDefaults.scrim, shape)
            } else {
                Modifier
            }
        )
        .semantics { if (hint != null) stateDescription = hint }
}

@Composable
private fun homeActionColors(): ButtonColors {
    val transparent = Color.Transparent
    return ButtonDefaults.buttonColors(
        containerColor = transparent,
        contentColor = Color.White,
        disabledContainerColor = transparent,
        disabledContentColor = Color.White.copy(alpha = .6f)
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
        modifier = Modifier.size(if (button.action == CardAction.Message) 32.dp else 26.dp),
        tint = if (button.action == CardAction.Message) {
            Color.White
        } else {
            tint.copy(alpha = if (button.enabled) 1f else .6f)
        }
    )
    Text(
        stringResource(button.label),
        modifier = Modifier.size(1.dp).graphicsLayer { alpha = 0f },
        style = MaterialTheme.typography.labelLarge
    )
}

internal fun HomeCardActions.perform(action: CardAction) = when (action) {
    CardAction.Pass -> passCurrentCard()
    CardAction.Message -> openTray()
    CardAction.Save -> saveCurrentCard()
}
