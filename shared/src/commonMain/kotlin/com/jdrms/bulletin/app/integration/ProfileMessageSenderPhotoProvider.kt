package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.MessageSenderPhotoProvider
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository

internal class ProfileMessageSenderPhotoProvider(
    private val profiles: ProfileRepository
) : MessageSenderPhotoProvider {
    override suspend fun photoUrl(senderId: SenderId): String? =
        when (val result = profiles.getProfile(UserId(senderId.value))) {
            is Result.Success -> result.data?.avatarUrl
            is Result.Error -> null
        }
}
