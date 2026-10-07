package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults

internal fun formatListingsConsequence(activeListingsCount: Int): String {
    return if (activeListingsCount > 0) {
        val unit = if (activeListingsCount == 1) "listing" else "listings"
        "$activeListingsCount active $unit, removed now"
    } else {
        "Active listings, removed now"
    }
}

internal fun formatHoldHint(holdDurationMillis: Int): String {
    val seconds = (holdDurationMillis / 1000.0).let {
        if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
    }
    return "Press and hold for $seconds seconds"
}

private fun isWithinBounds(position: Offset, size: IntSize): Boolean {
    val inHorizontalBounds = position.x in 0f..size.width.toFloat()
    val inVerticalBounds = position.y in 0f..size.height.toFloat()
    return inHorizontalBounds && inVerticalBounds
}

@Composable
internal fun DeleteProfileConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    activeListingsCount: Int = 0
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Delete your profile?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "This can’t be undone. Here’s what goes with it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                    ) {
                        ConsequenceRow(
                            icon = Icons.Outlined.LocalOffer,
                            text = formatListingsConsequence(activeListingsCount)
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        ConsequenceRow(
                            icon = Icons.AutoMirrored.Outlined.Chat,
                            text = "Messages, gone for other students too"
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        ConsequenceRow(
                            icon = Icons.Outlined.School,
                            text = "Campus verification, redo to rejoin"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = BulletinButtonDefaults.buttonColors()
                ) {
                    Text(
                        text = "Keep my profile",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                HoldToDeleteButton(
                    onConfirm = onConfirm,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ConsequenceRow(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HoldProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier
) {
    if (progress > 0f) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .background(MaterialTheme.colorScheme.error)
        )
    }
}

@Composable
private fun HoldButtonLabel(
    progress: Float,
    isPressed: Boolean
) {
    val label = when {
        progress >= 1f -> "Deleting…"
        isPressed -> "Keep holding…"
        else -> "Hold to delete"
    }
    val textColor = if (progress > 0.5f) {
        MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.error
    }
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = textColor
    )
}

@Composable
private fun HoldHintMessage(hintText: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .padding(top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (hintText != null) {
            Text(
                text = hintText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun HoldToDeleteButton(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    holdDurationMillis: Int = 1500
) {
    var isPressed by remember { mutableStateOf(false) }
    var hintText by remember { mutableStateOf<String?>(null) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            hintText = null
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = holdDurationMillis,
                    easing = LinearEasing
                )
            )
            if (progress.value >= 1f) {
                onConfirm()
            }
        } else {
            if (progress.value in 0.01f..0.99f) {
                hintText = formatHoldHint(holdDurationMillis)
            }
            progress.snapTo(0f)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(26.dp)
                )
                .semantics {
                    role = Role.Button
                    onClick(label = "Hold to delete profile") {
                        onConfirm()
                        true
                    }
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        val pointerId = down.id
                        while (isPressed) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            val shouldRelease = change == null || !change.pressed ||
                                !isWithinBounds(change.position, size)
                            if (shouldRelease) {
                                isPressed = false
                                break
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            HoldProgressIndicator(
                progress = progress.value,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            HoldButtonLabel(
                progress = progress.value,
                isPressed = isPressed
            )
        }

        HoldHintMessage(hintText = hintText)
    }
}
