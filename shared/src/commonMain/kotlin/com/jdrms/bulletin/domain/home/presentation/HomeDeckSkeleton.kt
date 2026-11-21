package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinSkeletonBlock
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

private const val SKELETON_TITLE_WIDTH = 0.6f
private const val SKELETON_PRICE_WIDTH = 0.3f

/**
 * Placeholder for the first Home card while the feed's first page loads. It uses the same frame as
 * [HomeFeedCard] so the real card replaces it without the layout shifting, and the shared design-system
 * skeleton so it shimmers like the other screens. The blocks hide their own semantics, so this container
 * announces a single indeterminate "loading" state, as the spinner it replaces did.
 */
@Composable
internal fun HomeDeckSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier.padding(BulletinSpacing.s).semantics(mergeDescendants = true) {
            contentDescription = "Loading listings"
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        }
    ) {
        BulletinSkeletonBlock(Modifier.fillMaxSize(), shape = MaterialTheme.shapes.large)
        Column(
            Modifier.align(Alignment.BottomStart).padding(BulletinSpacing.l),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
        ) {
            SkeletonBar(SKELETON_TITLE_WIDTH, 20.dp)
            SkeletonBar(SKELETON_PRICE_WIDTH, 16.dp)
        }
    }
}

/** A text-line stand-in drawn over the card block, tinted so it reads against the card's own fill. */
@Composable
private fun SkeletonBar(widthFraction: Float, height: Dp) {
    BulletinSkeletonBlock(
        Modifier.fillMaxWidth(widthFraction).height(height),
        shape = BulletinShapes.pill,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    )
}
