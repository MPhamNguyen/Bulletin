package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.jdrms.bulletin.core.designsystem.BulletinInitialsAvatar
import com.jdrms.bulletin.core.designsystem.skeleton
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes

@Composable
internal fun SellerAvatar(avatarUrl: String?, name: String?, isLoading: Boolean) {
    val avatarModifier = Modifier.size(84.dp)
        .skeleton(isLoading, shape = BulletinShapes.pill)
        .clip(BulletinShapes.pill)
    Box(avatarModifier) {
        BulletinInitialsAvatar(
            name.orEmpty(),
            Modifier.matchParentSize(),
            textStyle = MaterialTheme.typography.headlineMedium
        )
        if (!avatarUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = avatarUrl,
                contentDescription = "$name profile photo",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                loading = {},
                error = {}
            )
        }
    }
}
