package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.messages.domain.model.Message

@Composable
internal fun MessageSenderAvatar(message: Message, showAvatar: Boolean, onViewProfile: () -> Unit) {
    if (showAvatar) {
        MessageParticipantPhoto(
            message.displaySenderName,
            message.senderId,
            Modifier.clip(CircleShape).clickable(
                enabled = message.senderId != null,
                onClickLabel = "View profile",
                role = Role.Button,
                onClick = onViewProfile
            )
        )
    } else {
        Spacer(Modifier.width(40.dp))
    }
    Spacer(Modifier.width(BulletinSpacing.s))
}
