package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.domain.profile.domain.model.UserId

fun interface ProfileActiveListingsProvider {
    suspend fun getActiveListingsCount(userId: UserId): Int
}
