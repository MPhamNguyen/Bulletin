package com.jdrms.bulletin.domain.profile.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.UserId

/** Public avatar lookup without loading the profile or its reputation. */
fun interface PublicProfilePhotoRepository {
    suspend fun getAvatarUrl(userId: UserId): Result<String?>
}
