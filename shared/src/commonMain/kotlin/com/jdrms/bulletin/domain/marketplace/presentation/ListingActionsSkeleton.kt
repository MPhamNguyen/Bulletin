package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinSkeletonBlock
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/** Reserves the final messaging and action regions without enabling unverified actions. */
@Composable
internal fun ColumnScope.ListingActionsSkeleton(showBuyerActions: Boolean = true) {
    Spacer(Modifier.height(BulletinSpacing.xs))
    if (showBuyerActions) {
        Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
            Text("Message Seller", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BulletinSkeletonBlock(
                    Modifier.weight(1f).height(56.dp),
                    shape = MaterialTheme.shapes.medium
                )
                BulletinSkeletonBlock(
                    Modifier.width(80.dp).height(BulletinSpacing.control),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m)) {
        if (showBuyerActions) {
            BulletinSkeletonBlock(Modifier.weight(1f).height(BulletinSpacing.control))
        }
        BulletinSkeletonBlock(Modifier.weight(1f).height(BulletinSpacing.control))
    }
    if (showBuyerActions) {
        BulletinSkeletonBlock(Modifier.fillMaxWidth().height(BulletinSpacing.control))
    }
}
