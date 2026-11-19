package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinSkeletonBlock
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/**
 * Placeholder for the listing detail content. It mirrors the loaded layout (photo, title and
 * price, badges, seller card, description, item details) so the swap to real content is stable.
 * Emitted into the detail column, which supplies the spacing between sections.
 */
@Composable
internal fun ColumnScope.ListingDetailSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Loading listing details" },
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.l)
    ) {
        BulletinSkeletonBlock(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            shape = MaterialTheme.shapes.medium
        )
        SkeletonTitleRow()
        SkeletonBadgeRow()
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SellerInfoCardSkeleton()
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SkeletonTextLines(widths = DESCRIPTION_LINE_WIDTHS)
        SkeletonTextLines(widths = DETAIL_LINE_WIDTHS)
    }
}

@Composable
private fun SkeletonTitleRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BulletinSkeletonBlock(Modifier.fillMaxWidth(TITLE_WIDTH).height(28.dp))
        BulletinSkeletonBlock(Modifier.width(72.dp).height(28.dp))
    }
}

@Composable
private fun SkeletonBadgeRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
        BulletinSkeletonBlock(Modifier.width(88.dp).height(26.dp))
        BulletinSkeletonBlock(Modifier.width(72.dp).height(26.dp))
    }
}

@Composable
private fun SkeletonTextLines(widths: List<Float>) {
    Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
        widths.forEach { fraction ->
            BulletinSkeletonBlock(Modifier.fillMaxWidth(fraction).height(14.dp))
        }
    }
}

private const val TITLE_WIDTH = 0.55f
private val DESCRIPTION_LINE_WIDTHS = listOf(1f, 0.95f, 0.7f)
private val DETAIL_LINE_WIDTHS = listOf(0.45f, 0.6f, 0.5f)
