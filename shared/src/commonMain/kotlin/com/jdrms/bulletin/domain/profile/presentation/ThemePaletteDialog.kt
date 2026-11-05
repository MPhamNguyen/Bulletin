package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinButtonSize
import com.jdrms.bulletin.core.designsystem.BulletinSlider
import com.jdrms.bulletin.core.designsystem.theme.BulletinBasePalette
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
    var hexValue by remember(selectedBaseColor, selectedThemeColor) {
        mutableStateOf(workingBaseColor.toHexRgbDigits())
    }
    var selectedTab by remember { mutableStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a color") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxDialogContentHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    listOf("Base", "Accents").forEachIndexed { index, label ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                val color = if (index == 0) workingBaseColor else workingAccentColor
                                hexValue = color.toHexRgbDigits()
                            },
                            text = { Text(label) }
                        )
                    }
                }
                val workingColor = if (selectedTab == 0) workingBaseColor else workingAccentColor
                val (selectedHue, selectedSaturation, brightness) =
                    themeWheelHueSaturationAndBrightness(workingColor)
                BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val wheelSize = (maxWidth - 48.dp).coerceIn(180.dp, 260.dp)
                    ThemeColorWheel(
                        color = workingColor,
                        onColorSelected = {
                            if (selectedTab == 0) workingBaseColor = it else workingAccentColor = it
                            hexValue = it.toHexRgbDigits()
                        },
                        wheelDiameter = wheelSize
                    )
                }
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Brightness", style = MaterialTheme.typography.labelLarge)
                        Text("${(brightness * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                    }
                    BulletinSlider(
                        label = "Brightness",
                        value = brightness,
                        onValueChange = { value ->
                            val updated = themeWheelColorAt(selectedHue, selectedSaturation, value)
                            if (selectedTab == 0) workingBaseColor = updated else workingAccentColor = updated
                            hexValue = updated.toHexRgbDigits()
                        },
                        valueRange = 0f..1f
                    )
                }
                ThemeHexField(
                    value = hexValue,
                    onValueChange = { input ->
                        hexValue = input
                        workingColor.withHexRgb(input)?.let { updated ->
                            if (selectedTab == 0) workingBaseColor = updated else workingAccentColor = updated
                        }
                    }
                )
                if (selectedTab == 1) {
                    Text("Accent presets", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        BulletinPalette.entries.forEach { palette ->
                            PresetOption(
                                label = palette.label,
                                color = palette.color,
                                selected = workingAccentColor == palette.color,
                                onClick = {
                                    workingAccentColor = palette.color
                                    hexValue = palette.color.toHexRgbDigits()
                                }
                            )
                        }
                    }
                } else {
                    Text("Base presets", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        BulletinBasePalette.entries.forEach { palette ->
                            PresetOption(
                                label = palette.label,
                                color = palette.color,
                                selected = workingBaseColor == palette.color,
                                onClick = {
                                    workingBaseColor = palette.color
                                    hexValue = palette.color.toHexRgbDigits()
                                }
                            )
                        }
                    }
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

private val maxDialogContentHeight = 420.dp

@Composable
private fun PresetOption(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics {
                contentDescription = "$label preset"
                stateDescription = if (selected) "Selected" else "Not selected"
            }
            .padding(horizontal = BulletinSpacing.xs, vertical = BulletinSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color, CircleShape)
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
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
