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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.app.theme.ThemePreference
import com.jdrms.bulletin.core.designsystem.BulletinCard

@Composable
internal fun AppearanceCard(themePreference: ThemePreference, onThemePreferenceChanged: (ThemePreference) -> Unit) {
    BulletinCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Brush,
                    contentDescription = "Appearance",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
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
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
        tween(durationMillis = 180),
        label = "Appearance option background"
    )
    val borderColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        tween(durationMillis = 180),
        label = "Appearance option border"
    )
    val lift by animateDpAsState(
        if (selected) (-2).dp else 0.dp,
        tween(durationMillis = 180),
        label = "Appearance option lift"
    )
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = modifier
            .height(76.dp)
            .graphicsLayer { translationY = lift.toPx() }
            .background(backgroundColor, MaterialTheme.shapes.medium)
            .border(BorderStroke(1.dp, borderColor), MaterialTheme.shapes.medium)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics {
                contentDescription = themePreferenceDescription(preference)
                stateDescription = if (selected) "Selected" else "Not selected"
            }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            themePreferenceIcon(preference),
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
