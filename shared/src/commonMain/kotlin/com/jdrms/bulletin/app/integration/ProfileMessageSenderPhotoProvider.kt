package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.MessageSenderPhotoProvider
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.PublicProfilePhotoRepository

internal class ProfileMessageSenderPhotoProvider(
    private val profiles: PublicProfilePhotoRepository
) : MessageSenderPhotoProvider {
    override suspend fun photoUrl(senderId: SenderId): String? =
        when (val result = profiles.getAvatarUrl(UserId(senderId.value))) {
            is Result.Success -> result.data
            is Result.Error -> null
        }
}
