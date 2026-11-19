package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinSkeletonBlock
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/** Keeps the seller card's shell visible while only its unknown fields shimmer. */
@Composable
internal fun SellerInfoCardSkeleton(name: String? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = "Loading seller details"
        },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(
                start = BulletinSpacing.l,
                top = BulletinSpacing.l,
                bottom = BulletinSpacing.l,
                end = BulletinSpacing.m
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
        ) {
            BulletinSkeletonBlock(Modifier.size(84.dp), BulletinShapes.pill)
            SellerInfoSkeletonFields(name, Modifier.weight(1f))
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun SellerInfoSkeletonFields(name: String?, modifier: Modifier) {
    val typography = MaterialTheme.typography
    val titleHeight = with(LocalDensity.current) { typography.titleLarge.lineHeight.toDp() }
    val schoolHeight = with(LocalDensity.current) { typography.bodyMedium.lineHeight.toDp() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (name == null) {
            BulletinSkeletonBlock(Modifier.fillMaxWidth(0.8f).height(titleHeight))
        } else {
            Text(
                name,
                style = typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        BulletinSkeletonBlock(Modifier.fillMaxWidth().height(schoolHeight))
        Row(horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)) {
            BulletinSkeletonBlock(Modifier.weight(1f).height(schoolHeight))
            BulletinSkeletonBlock(Modifier.weight(1f).height(schoolHeight))
        }
    }
}
