package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.theme.BulletinStatusDefaults
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreview
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

internal fun buyerSaleNotice(
    preview: ConversationListingPreview?,
    viewerId: SenderId?,
    seller: ConversationParticipant?
): String? {
    if (preview?.isSold != true || viewerId == null || seller == null) return null
    if (preview.soldToBuyerId != viewerId.value || preview.sellerId != seller.id.value) return null
    if (preview.sellerId == viewerId.value) return null
    return "${seller.displayName} sold ${preview.title} to you"
}

@Composable
internal fun BuyerSaleNotice(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = BulletinSpacing.m, vertical = BulletinSpacing.s),
        shape = MaterialTheme.shapes.medium,
        color = BulletinStatusDefaults.containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Text(message, modifier = Modifier.padding(BulletinSpacing.m), style = MaterialTheme.typography.bodyMedium)
    }
}
