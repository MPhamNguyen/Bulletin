package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinSkeletonBlock
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/** Placeholder mirroring the seller profile hero, stats, and about cards. */
@Composable
internal fun SellerProfileSkeleton(preview: SellerProfilePreview? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = BulletinSpacing.l)
            .semantics { contentDescription = "Loading seller profile" },
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.l)
    ) {
        BulletinCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
            ) {
                if (preview != null) {
                    SellerAvatar(preview.name, preview.avatarUrl)
                } else {
                    BulletinSkeletonBlock(Modifier.size(AVATAR_SIZE), BulletinShapes.pill)
                }
                if (!preview?.name.isNullOrBlank()) {
                    Text(
                        text = preview?.name.orEmpty(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    BulletinSkeletonBlock(Modifier.width(160.dp).height(24.dp))
                }
                BulletinSkeletonBlock(Modifier.width(200.dp).height(16.dp))
            }
        }
        BulletinCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SkeletonStat()
                SkeletonStat()
            }
        }
        BulletinCard {
            Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
                BulletinSkeletonBlock(Modifier.width(80.dp).height(18.dp))
                BulletinSkeletonBlock(Modifier.fillMaxWidth().height(14.dp))
                BulletinSkeletonBlock(Modifier.fillMaxWidth(ABOUT_LAST_LINE).height(14.dp))
            }
        }
    }
}

@Composable
private fun SkeletonStat() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)
    ) {
        BulletinSkeletonBlock(Modifier.width(48.dp).height(24.dp))
        BulletinSkeletonBlock(Modifier.width(80.dp).height(14.dp))
    }
}

private val AVATAR_SIZE = 88.dp
private const val ABOUT_LAST_LINE = 0.6f
