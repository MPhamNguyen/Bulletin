package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun ConversationUnreadIndicator() {
    Box(
        modifier = Modifier.size(14.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
            .semantics { contentDescription = "Unread message" },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(7.dp)
                .shadow(5.dp, CircleShape)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
    }
}
