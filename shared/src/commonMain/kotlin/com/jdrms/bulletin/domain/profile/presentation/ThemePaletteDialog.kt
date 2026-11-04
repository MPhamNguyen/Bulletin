package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinButtonSize
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinPalette
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.theme.BulletinThemeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemePaletteDialog(
    selectedBaseColor: BulletinThemeColor,
    selectedThemeColor: BulletinThemeColor,
    onThemeColorsSelected: (BulletinThemeColor, BulletinThemeColor) -> Unit,
    onDismiss: () -> Unit
) {
    var workingBaseColor by remember(selectedBaseColor) { mutableStateOf(selectedBaseColor.color) }
    var workingAccentColor by remember(selectedThemeColor) { mutableStateOf(selectedThemeColor.color) }
    var selectedTab by remember { mutableStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a color") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    listOf("Base", "Accents").forEachIndexed { index, label ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(label) }
                        )
                    }
                }
                val workingColor = if (selectedTab == 0) workingBaseColor else workingAccentColor
                BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val wheelSize = (maxWidth - 48.dp).coerceIn(180.dp, 260.dp)
                    ThemeColorWheel(
                        color = workingColor,
                        onColorSelected = {
                            if (selectedTab == 0) workingBaseColor = it else workingAccentColor = it
                        },
                        wheelDiameter = wheelSize
                    )
                }
                if (selectedTab == 1) {
                    Text("Accent presets", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        BulletinPalette.entries.forEach { palette ->
                            PresetOption(
                                palette = palette,
                                selected = workingAccentColor == palette.color,
                                onClick = { workingAccentColor = palette.color }
                            )
                        }
                    }
                } else {
                    Text("Choose a background tone", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        confirmButton = {
            BulletinButton(
                onClick = {
                    onThemeColorsSelected(
                        BulletinThemeColor(workingBaseColor.value),
                        BulletinThemeColor(workingAccentColor.value)
                    )
                    onDismiss()
                },
                variant = BulletinButtonVariant.Primary,
                size = BulletinButtonSize.Regular
            ) { Text("Done") }
        },
        dismissButton = {
            BulletinButton(
                onClick = onDismiss,
                variant = BulletinButtonVariant.Text,
                size = BulletinButtonSize.Regular
            ) { Text("Cancel") }
        }
    )
}

@Composable
private fun PresetOption(palette: BulletinPalette, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics {
                contentDescription = "${palette.label} preset"
                stateDescription = if (selected) "Selected" else "Not selected"
            }
            .padding(horizontal = BulletinSpacing.xs, vertical = BulletinSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(palette.color, CircleShape)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = CircleShape
                )
        )
        Text(palette.label, style = MaterialTheme.typography.labelSmall)
    }
}
