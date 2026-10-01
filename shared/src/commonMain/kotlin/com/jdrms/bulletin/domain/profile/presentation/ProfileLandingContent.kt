package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jdrms.bulletin.app.theme.ThemePreference
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinExtras

@Composable
internal fun ProfileLandingView(
    uiState: ProfileUiState,
    onEditProfileClick: () -> Unit,
    onMyListingsClick: () -> Unit,
    themePreference: ThemePreference?,
    onThemePreferenceChanged: ((ThemePreference) -> Unit)?,
    onSignOut: () -> Unit
) {
    val profile = uiState.profile

    Column(modifier = Modifier.fillMaxSize()) {
        ProfileLandingTopBar(onEditProfileClick = onEditProfileClick)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 2.dp, end = 20.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileAvatarHeader(
                fullName = profile?.fullName ?: "Student Profile",
                university = profile?.university ?: "CSU Long Beach",
                isVerified = profile?.isVerified == true || profile?.email?.isUniversityEmail == true
            )

            Spacer(modifier = Modifier.height(16.dp))

            BulletinCard {
                ProfileSectionLabel("ABOUT")
                Spacer(modifier = Modifier.height(8.dp))
                val bioText = profile?.bio?.ifBlank { null }
                    ?: "No bio added yet. Tap Edit to tell fellow students about yourself!"
                Text(
                    text = bioText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (profile?.bio.isNullOrBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    },
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            BulletinCard {
                ProfileSectionLabel("ACADEMIC INFORMATION")
                Spacer(modifier = Modifier.height(12.dp))
                ProfileInfoRow(
                    label = "School",
                    value = profile?.university?.ifBlank { null } ?: "CSU Long Beach"
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                )
                ProfileInfoRow(
                    label = "Major",
                    value = profile?.major?.ifBlank { null } ?: "Undeclared"
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                )
                ProfileInfoRow(
                    label = "Email",
                    value = profile?.email?.value ?: "No email linked"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            BulletinCard {
                ProfileSectionLabel("CAMPUS REPUTATION")
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                                MaterialTheme.shapes.medium
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Average Rating",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = BulletinExtras.colors.star,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = (uiState.reputation?.averageRating ?: 5.0).toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                                MaterialTheme.shapes.medium
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Feedback",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${uiState.reputation?.totalReviews ?: 0} reviews",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ProfileActionRow(
                icon = Icons.Outlined.GridView,
                title = "My Listings",
                onClick = onMyListingsClick
            )

            if (themePreference != null && onThemePreferenceChanged != null) {
                Spacer(modifier = Modifier.height(16.dp))
                AppearanceCard(
                    themePreference = themePreference,
                    onThemePreferenceChanged = onThemePreferenceChanged
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SignOutButton(onClick = onSignOut)
        }
    }
}

@Composable
private fun AppearanceCard(
    themePreference: ThemePreference,
    onThemePreferenceChanged: (ThemePreference) -> Unit
) {
    BulletinCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Brush,
                    contentDescription = "Appearance",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemePreference.entries.forEach { preference ->
                AppearanceOption(
                    preference = preference,
                    selected = preference == themePreference,
                    onClick = { onThemePreferenceChanged(preference) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AppearanceOption(
    preference: ThemePreference,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(durationMillis = 180),
        label = "Appearance option background"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(durationMillis = 180),
        label = "Appearance option border"
    )
    val lift by animateDpAsState(
        targetValue = if (selected) (-2).dp else 0.dp,
        animationSpec = tween(durationMillis = 180),
        label = "Appearance option lift"
    )
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .height(76.dp)
            .graphicsLayer { translationY = lift.toPx() }
            .background(backgroundColor, MaterialTheme.shapes.medium)
            .border(BorderStroke(1.dp, borderColor), MaterialTheme.shapes.medium)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .semantics {
                contentDescription = themePreferenceDescription(preference)
                stateDescription = if (selected) "Selected" else "Not selected"
            }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = themePreferenceIcon(preference),
            contentDescription = themePreferenceLabel(preference),
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = themePreferenceLabel(preference),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1
        )
    }
}

private fun themePreferenceLabel(preference: ThemePreference): String = when (preference) {
    ThemePreference.SYSTEM -> "System"
    ThemePreference.LIGHT -> "Light"
    ThemePreference.DARK -> "Dark"
}

private fun themePreferenceDescription(preference: ThemePreference): String = when (preference) {
    ThemePreference.SYSTEM -> "System theme, follows your device setting"
    ThemePreference.LIGHT -> "Light theme, bright and clean for daylight"
    ThemePreference.DARK -> "Dark theme, deep navy for low light"
}

private fun themePreferenceIcon(preference: ThemePreference) = when (preference) {
    ThemePreference.SYSTEM -> Icons.Outlined.PhoneIphone
    ThemePreference.LIGHT -> Icons.Outlined.LightMode
    ThemePreference.DARK -> Icons.Outlined.DarkMode
}

internal fun profileInitials(fullName: String): String {
    val nameParts = fullName.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    if (nameParts.isEmpty()) return "ST"
    val firstInitial = nameParts.first().first().uppercaseChar()
    val lastInitial = nameParts.takeIf { it.size > 1 }?.last()?.first()?.uppercaseChar()
    return if (lastInitial == null) firstInitial.toString() else "$firstInitial$lastInitial"
}

@Composable
private fun ProfileSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ProfileLandingTopBar(onEditProfileClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        TextButton(
            onClick = onEditProfileClick,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Text(
                text = "Edit",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ProfileAvatarHeader(
    fullName: String,
    university: String,
    isVerified: Boolean
) {
    Box {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = profileInitials(fullName),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        if (isVerified) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(MaterialTheme.colorScheme.background, CircleShape)
                    .padding(3.dp)
                    .size(22.dp)
                    .background(BulletinExtras.colors.successContainer, CircleShape)
                    .border(1.dp, BulletinExtras.colors.success.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Verified student",
                    tint = BulletinExtras.colors.success,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = fullName,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = university,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    if (isVerified) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .background(
                    color = BulletinExtras.colors.successContainer,
                    shape = CircleShape
                )
                .border(1.dp, BulletinExtras.colors.success.copy(alpha = 0.2f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = BulletinExtras.colors.success,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Verified Student",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = BulletinExtras.colors.success
            )
        }
    }
}

@Composable
private fun ProfileActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileActionIcon(icon)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ProfileActionIcon(
    icon: ImageVector
) {
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(color.copy(alpha = 0.1f), MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
