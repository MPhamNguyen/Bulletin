package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/** Placeholder block for callers that reserve their own dimensions. */
@Composable
fun BulletinSkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    color: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Box(modifier.skeleton(visible = true, shape = shape, color = color))
}
