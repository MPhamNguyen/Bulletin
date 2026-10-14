package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.domain.profile.domain.model.UserId

fun interface ProfileSoldListingsProvider {
    suspend fun getSoldListingsCount(userId: UserId): Int
}
