package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinSkeletonBlock
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/** Category and condition placeholders matching the loaded tags' shape and text-scaled height. */
@Composable
internal fun ListingTagsSkeleton() {
    val tagHeight = with(LocalDensity.current) { MaterialTheme.typography.labelMedium.lineHeight.toDp() } +
        BulletinSpacing.s
    FlowRow(
        modifier = Modifier.semantics { contentDescription = "Loading category and condition" },
        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s),
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
    ) {
        BulletinSkeletonBlock(Modifier.width(88.dp).height(tagHeight), shape = MaterialTheme.shapes.small)
        BulletinSkeletonBlock(
            Modifier.width(72.dp).height(tagHeight),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer
        )
    }
}
