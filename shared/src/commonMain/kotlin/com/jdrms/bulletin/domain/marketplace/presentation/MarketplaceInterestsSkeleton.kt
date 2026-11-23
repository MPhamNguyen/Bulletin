package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinSkeletonBlock
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/** Placeholder for the category chips shown while the saved interests load. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MarketplaceInterestsSkeleton() {
    FlowRow(
        modifier = Modifier.semantics { contentDescription = "Loading interests" },
        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s),
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
    ) {
        CHIP_WIDTHS.forEach { width ->
            BulletinSkeletonBlock(Modifier.width(width).height(32.dp), BulletinShapes.pill)
        }
    }
}

private val CHIP_WIDTHS = listOf(96.dp, 72.dp, 112.dp, 84.dp, 104.dp, 68.dp, 92.dp, 80.dp)
