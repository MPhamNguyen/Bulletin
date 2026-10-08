package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jdrms.bulletin.app.theme.ThemePreference
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile

internal data class SettingsActions(
    val onEditAccount: () -> Unit,
    val onViewPublicProfile: () -> Unit,
    val onNotifications: () -> Unit,
    val onPrivacy: () -> Unit,
    val onHelpSupport: () -> Unit,
    val onTermsConditions: () -> Unit,
    val onSignOut: () -> Unit,
    val onDeleteProfile: () -> Unit = {},
    val onConfirmDeleteProfile: () -> Unit = {}
)

@Composable
internal fun SettingsView(
    uiState: ProfileUiState,
    onBack: () -> Unit,
    actions: SettingsActions,
    themePreference: ThemePreference? = null,
    onThemePreferenceChanged: ((ThemePreference) -> Unit)? = null,
    showDeleteConfirmation: Boolean? = null
) {
    var localShowDeleteConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    val isDeleteDialogOpen = showDeleteConfirmation ?: localShowDeleteConfirmationDialog

    if (isDeleteDialogOpen) {
        DeleteProfileConfirmationDialog(
            onDismiss = {
                localShowDeleteConfirmationDialog = false
            },
            onConfirm = {
                localShowDeleteConfirmationDialog = false
                actions.onConfirmDeleteProfile()
            }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SettingsTopBar(onBackClick = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // First and most prominent option at the top of the list: Edit Account
            SettingsEditAccountCard(
                profile = uiState.profile,
                onClick = actions.onEditAccount
            )

            SettingsActionCard(
                icon = Icons.Outlined.Visibility,
                title = "View Public Profile",
                description = "Preview how your profile and campus listings appear to others",
                onClick = actions.onViewPublicProfile
            )

            // Section: Preferences
            SettingsSectionHeader(title = "PREFERENCES")
            SettingsActionCard(
                icon = Icons.Outlined.Notifications,
                title = "Notifications",
                description = "Push alerts for chat messages, offers, and price drops",
                onClick = actions.onNotifications
            )

            if (themePreference != null && onThemePreferenceChanged != null) {
                AppearanceCard(
                    themePreference = themePreference,
                    onThemePreferenceChanged = onThemePreferenceChanged
                )
            }

            // Section: Security & Privacy
            SettingsSectionHeader(title = "SECURITY & PRIVACY")
            SettingsActionCard(
                icon = Icons.Outlined.Security,
                title = "Privacy",
                description = "Control profile visibility, campus verification, and student email",
                onClick = actions.onPrivacy
            )

            // Section: Support & Legal
            SettingsSectionHeader(title = "SUPPORT & LEGAL")
            SettingsActionCard(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                title = "Help and Support",
                description = "Campus meetup safety guidelines, FAQs, and support team",
                onClick = actions.onHelpSupport
            )
            SettingsActionCard(
                icon = Icons.Outlined.Description,
                title = "Terms and Conditions",
                description = "Bulletin student honor code, community standards, and marketplace rules",
                onClick = actions.onTermsConditions
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Destructive actions at the bottom of the list
            SignOutButton(onClick = actions.onSignOut)
            DeleteProfileButton(
                onClick = {
                    localShowDeleteConfirmationDialog = true
                    actions.onDeleteProfile()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back to Profile",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun SettingsEditAccountCard(
    profile: StudentProfile?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatarImage(
                avatarUrl = profile?.avatarUrl,
                fullName = profile?.fullName ?: "Dominic Alfonso",
                size = 54.dp,
                borderWidth = 1.5.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Edit Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = profile?.fullName?.ifBlank { null } ?: "Dominic Alfonso",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Update name, major, graduation date & bio",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    BulletinCard {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
internal fun DeleteProfileButton(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = MaterialTheme.shapes.large,
        border = BulletinButtonDefaults.destructiveOutlinedButtonBorder(),
        colors = BulletinButtonDefaults.destructiveOutlinedButtonColors()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Delete Profile",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
