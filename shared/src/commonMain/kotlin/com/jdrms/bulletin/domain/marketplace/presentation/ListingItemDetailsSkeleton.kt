package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinSkeletonBlock
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/** Label/value placeholders follow ItemDetailsSection's column widths and line spacing. */
@Composable
internal fun ListingItemDetailsSkeleton() {
    val rowHeight = with(LocalDensity.current) { MaterialTheme.typography.bodyMedium.lineHeight.toDp() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Item details", style = MaterialTheme.typography.titleMedium)
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m)) {
                BulletinSkeletonBlock(Modifier.width(120.dp).height(rowHeight))
                BulletinSkeletonBlock(Modifier.fillMaxWidth(0.6f).height(rowHeight))
            }
        }
    }
}
