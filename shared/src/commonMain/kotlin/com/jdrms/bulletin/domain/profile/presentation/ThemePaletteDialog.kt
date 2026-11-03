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
import androidx.compose.material3.MaterialTheme
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

@Composable
internal fun ThemePaletteDialog(
    selectedThemeColor: BulletinThemeColor,
    onThemeColorSelected: (BulletinThemeColor) -> Unit,
    onDismiss: () -> Unit
) {
    var workingColor by remember(selectedThemeColor) { mutableStateOf(selectedThemeColor.color) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a color") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val wheelSize = (maxWidth - 48.dp).coerceIn(180.dp, 260.dp)
                    ThemeColorWheel(
                        color = workingColor,
                        onColorSelected = { workingColor = it },
                        wheelDiameter = wheelSize
                    )
                }
                Text("Presets", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BulletinPalette.entries.forEach { palette ->
                        PresetOption(
                            palette = palette,
                            selected = workingColor == palette.color,
                            onClick = { workingColor = palette.color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            BulletinButton(
                onClick = {
                    onThemeColorSelected(BulletinThemeColor(workingColor.value))
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
