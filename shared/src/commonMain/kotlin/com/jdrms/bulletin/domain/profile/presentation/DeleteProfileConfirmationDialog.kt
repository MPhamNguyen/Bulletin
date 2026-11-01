package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinButtonSize
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

private const val PROFILE_SOFT_DELETE_CONSEQUENCE = "Your profile is marked for soft-delete"

@Composable
internal fun DeleteProfileConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp),
            shape = BulletinShapes.dialog,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = BulletinSpacing.xl),
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

                Spacer(modifier = Modifier.height(BulletinSpacing.m))

                Text(
                    text = "Delete your profile?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Your profile will be marked as deleted and you’ll be signed out.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(BulletinSpacing.l))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = BulletinShapes.inset,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                    ) {
                        ConsequenceRow(
                            icon = Icons.Outlined.LocalOffer,
                            text = PROFILE_SOFT_DELETE_CONSEQUENCE
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        ConsequenceRow(
                            icon = Icons.AutoMirrored.Outlined.Chat,
                            text = "Your listings and messages are not changed"
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        ConsequenceRow(
                            icon = Icons.Outlined.School,
                            text = "You can contact support if you need help later"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                BulletinButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth(),
                    variant = BulletinButtonVariant.Primary,
                    size = BulletinButtonSize.Large
                ) {
                    Text(
                        text = "Keep my profile",
                        style = MaterialTheme.typography.titleSmall,
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
            .padding(vertical = BulletinSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
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
