package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.BulletinInitialsAvatar

@Composable
internal fun ConversationAvatar(name: String, modifier: Modifier = Modifier) {
    BulletinInitialsAvatar(name, modifier.size(MessageAvatarSize))
}
