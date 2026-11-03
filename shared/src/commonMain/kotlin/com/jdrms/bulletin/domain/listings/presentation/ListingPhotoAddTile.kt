package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal val ListingPhotoTileSize = 92.dp

/** Dashed upload tile sized like a photo thumbnail; the whole tile opens the photo picker. */
@Composable
internal fun ListingPhotoAddTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.small
    val scheme = MaterialTheme.colorScheme
    val borderColor = scheme.secondary
    val density = androidx.compose.ui.platform.LocalDensity.current
    Column(
        modifier = modifier
            .size(ListingPhotoTileSize)
            .clip(shape)
            .background(scheme.primaryContainer)
            .drawBehind {
                val stroke = with(density) { 1.5.dp.toPx() }
                val dash = with(density) { 6.dp.toPx() }
                drawRoundRect(
                    color = borderColor,
                    topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)))
                )
            }
            .clickable(role = Role.Button, onClickLabel = "Add photos", onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = "Add photos",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = scheme.primary
        )
    }
}
