package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.theme.bulletinColors

private fun ratingLabel(rating: Int): String = when (rating) {
    1 -> "Poor (1.0)"
    2 -> "Fair (2.0)"
    3 -> "Good (3.0)"
    4 -> "Very Good (4.0)"
    else -> "Excellent (5.0)"
}

@Composable
internal fun BuyerRatingStars(rating: Int, onRatingChange: (Int) -> Unit) {
    Column(
        modifier = Modifier.padding(top = 14.dp).fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .padding(vertical = BulletinSpacing.s),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
            for (value in 1..5) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = if (value == 1) "1 star" else "$value stars",
                    tint = if (value <= rating) {
                        MaterialTheme.bulletinColors.star
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(36.dp).clickable { onRatingChange(value) }.padding(4.dp)
                )
            }
        }
        Text(
            ratingLabel(rating),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = BulletinSpacing.xs),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
