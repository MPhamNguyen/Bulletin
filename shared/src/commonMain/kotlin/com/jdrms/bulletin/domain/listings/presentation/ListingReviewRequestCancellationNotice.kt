package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.listings.domain.model.Listing

@Composable
internal fun ListingReviewRequestCancellationNotice(
    listing: Listing,
    isRetrying: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(BulletinSpacing.m),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(BulletinSpacing.m),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)) {
                Text(
                    "${listing.title} was restored, but its review requests still need cleanup.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
                errorMessage?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            BulletinButton(
                onClick = onRetry,
                enabled = !isRetrying,
                variant = BulletinButtonVariant.Text
            ) {
                Text(if (isRetrying) "Retrying…" else "Retry")
            }
        }
    }
}
