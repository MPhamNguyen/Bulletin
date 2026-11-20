package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.theme.bulletinColors
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreview
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

/** Returns the seller's display name when the viewer is the chosen buyer and should be asked to rate them. */
internal fun buyerRatingPromptSeller(
    preview: ConversationListingPreview?,
    viewerId: SenderId?,
    seller: ConversationParticipant?
): String? {
    if (preview?.isSold != true || viewerId == null || seller == null) return null
    if (preview.soldToBuyerId != viewerId.value || preview.sellerId != seller.id.value) return null
    if (preview.sellerId == viewerId.value) return null
    return seller.displayName
}

private const val MAX_COMMENT_LENGTH = 300
private const val DEFAULT_RATING = 5

@Composable
internal fun BuyerSaleNotice(
    sellerName: String,
    onSubmit: (rating: Int, comment: String) -> Unit = { _, _ -> }
) {
    var dismissed by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var rating by remember { mutableStateOf(DEFAULT_RATING) }
    var comment by remember { mutableStateOf("") }
    if (dismissed) return

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = BulletinSpacing.m, vertical = BulletinSpacing.s),
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            Modifier.background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)
                )
            )
        ) {
            if (submitted) {
                BuyerRatingSubmitted(sellerName)
            } else {
                Column(Modifier.padding(BulletinSpacing.l)) {
                    BuyerRatingHeader(sellerName) { dismissed = true }
                    BuyerRatingStars(rating) { rating = it }
                    BuyerRatingComment(comment) { comment = it.take(MAX_COMMENT_LENGTH) }
                    BuyerRatingSubmitButton {
                        onSubmit(rating, comment.trim())
                        submitted = true
                    }
                }
            }
        }
    }
}

@Composable
private fun BuyerRatingHeader(sellerName: String, onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                "How was your experience?",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                "Rate $sellerName for this transaction",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Icon(
            Icons.Filled.Close,
            contentDescription = "Dismiss",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp).clickable(onClick = onDismiss).padding(4.dp)
        )
    }
}

@Composable
private fun BuyerRatingSubmitted(sellerName: String) {
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(BulletinSpacing.l),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(40.dp)
                .background(MaterialTheme.bulletinColors.success.copy(alpha = 0.20f), BulletinShapes.pill)
                .border(1.dp, MaterialTheme.bulletinColors.success.copy(alpha = 0.40f), BulletinShapes.pill),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.bulletinColors.success,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            "Review Submitted!",
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = BulletinSpacing.s),
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            "Thank you for rating $sellerName.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
