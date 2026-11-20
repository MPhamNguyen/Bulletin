package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.theme.bulletinColors

private const val OUTLINE_ALPHA = 0.35f

@Composable
internal fun ListingActiveBadge(modifier: Modifier = Modifier) {
    val color = MaterialTheme.bulletinColors.success
    Surface(
        modifier = modifier,
        shape = BulletinShapes.pill,
        color = MaterialTheme.bulletinColors.successContainer,
        contentColor = MaterialTheme.bulletinColors.onSuccessContainer,
        border = BorderStroke(1.dp, color.copy(alpha = OUTLINE_ALPHA))
    ) {
        Text(
            "Listing Active",
            modifier = Modifier.padding(horizontal = BulletinSpacing.s, vertical = BulletinSpacing.xs / 2),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
internal fun SellToAction(buyerName: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = MaterialTheme.bulletinColors.success
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
        shape = BulletinShapes.pill,
        color = color.copy(alpha = 0.08f),
        contentColor = color,
        border = BorderStroke(1.dp, color.copy(alpha = OUTLINE_ALPHA))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = BulletinSpacing.m, vertical = BulletinSpacing.s),
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                "Sell to $buyerName",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
