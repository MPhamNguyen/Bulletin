package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.messages.domain.model.ConversationLockReason

@Composable
internal fun ReadOnlyConversationNotice(reason: ConversationLockReason) {
    Text(
        reason.description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(BulletinSpacing.l)
    )
}
