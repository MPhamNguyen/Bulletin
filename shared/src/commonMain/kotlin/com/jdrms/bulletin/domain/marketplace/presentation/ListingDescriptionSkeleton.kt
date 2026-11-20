package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinSkeletonBlock

/** Matches the description heading, body line boxes and timestamp in the final section. */
@Composable
internal fun ListingDescriptionSkeleton() {
    val density = LocalDensity.current
    val bodyHeight = with(density) { MaterialTheme.typography.bodyMedium.lineHeight.toDp() }
    val timeHeight = with(density) { MaterialTheme.typography.bodySmall.lineHeight.toDp() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("About this item", style = MaterialTheme.typography.titleMedium)
        Column {
            listOf(1f, 0.95f, 0.7f).forEach { width ->
                BulletinSkeletonBlock(Modifier.fillMaxWidth(width).height(bodyHeight))
            }
        }
        BulletinSkeletonBlock(Modifier.fillMaxWidth(0.45f).height(timeHeight))
    }
}
