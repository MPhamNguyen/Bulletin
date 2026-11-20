package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.jdrms.bulletin.core.designsystem.skeleton
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes

@Composable
internal fun SellerAvatar(avatarUrl: String?, name: String?, isLoading: Boolean) {
    val avatarModifier = Modifier.size(84.dp)
        .skeleton(isLoading, shape = BulletinShapes.pill)
        .clip(BulletinShapes.pill)
    if (avatarUrl != null) {
        SubcomposeAsyncImage(
            model = avatarUrl,
            contentDescription = "$name profile photo",
            modifier = avatarModifier,
            contentScale = ContentScale.Crop,
            loading = { Box(Modifier.size(84.dp).skeleton(true, shape = BulletinShapes.pill)) }
        )
    } else {
        Box(
            modifier = avatarModifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "Seller Avatar",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}
