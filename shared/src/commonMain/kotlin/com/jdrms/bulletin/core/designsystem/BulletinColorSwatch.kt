package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/** A named color choice with a full touch target and an independent selection ring. */
@Composable
fun BulletinColorSwatch(code: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = swatchColors(code)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(BulletinSpacing.control)
            .semantics { contentDescription = label }
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(BulletinSpacing.xs)
            .border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = BulletinShapes.pill
            )
            .padding(BulletinSpacing.xs)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .background(Brush.linearGradient(colors), BulletinShapes.pill)
                .border(1.dp, MaterialTheme.colorScheme.outline, BulletinShapes.pill)
        ) {
            if (code == "other") {
                Text("?", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private fun swatchColors(code: String): List<Color> {
    if (code == "multicolor") {
        return listOf(Color(0xFFE53935), Color(0xFFFDD835), Color(0xFF43A047), Color(0xFF1E88E5))
    }
    val color = swatchPalette[code] ?: Color.Transparent
    return listOf(color, color)
}

private val swatchPalette = mapOf(
    "black" to Color(0xFF202020),
    "white" to Color.White,
    "gray" to Color(0xFF888888),
    "red" to Color(0xFFE53935),
    "orange" to Color(0xFFFB8C00),
    "yellow" to Color(0xFFFDD835),
    "green" to Color(0xFF43A047),
    "blue" to Color(0xFF1E88E5),
    "purple" to Color(0xFF8E24AA),
    "pink" to Color(0xFFF48FB1),
    "brown" to Color(0xFF795548),
    "beige" to Color(0xFFE3D3B4)
)
