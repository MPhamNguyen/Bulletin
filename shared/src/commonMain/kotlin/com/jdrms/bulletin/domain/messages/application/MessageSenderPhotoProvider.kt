package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.domain.messages.domain.model.SenderId

/** Messages-owned boundary for the sender's public profile photo. */
fun interface MessageSenderPhotoProvider {
    suspend fun photoUrl(senderId: SenderId): String?
}
