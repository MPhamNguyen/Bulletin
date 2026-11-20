package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/** Shows the card's photo in the hero slot, when there is one, so it is already in place as the page opens. */
@Composable
internal fun ListingDetailsLoading(previewPhotoUrl: String?, photoModifier: Modifier) {
    if (previewPhotoUrl != null) {
        Box(photoModifier) { ListingPhotosSection(photos = listOf(previewPhotoUrl)) }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (previewPhotoUrl == null) 260.dp else 120.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Text(
                text = "Loading listing details...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun ListingDetailsError(errorMessage: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = BulletinSpacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(BulletinSpacing.xs))
            BulletinButton(
                onClick = onRetry,
                variant = BulletinButtonVariant.Primary
            ) {
                Text("Retry")
            }
        }
    }
}
