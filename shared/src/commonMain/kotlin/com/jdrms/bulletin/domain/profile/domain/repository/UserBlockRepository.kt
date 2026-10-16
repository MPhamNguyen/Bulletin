package com.jdrms.bulletin.domain.profile.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.UserBlock
import com.jdrms.bulletin.domain.profile.domain.model.UserId

interface UserBlockRepository {
    suspend fun block(block: UserBlock): Result<Unit>

    suspend fun unblock(block: UserBlock): Result<Unit>

    /** Users explicitly blocked by [viewerId]. */
    suspend fun getBlockedUserIds(viewerId: UserId): Result<Set<UserId>>

    /** Users whose listings must be hidden from [viewerId]. */
    suspend fun getHiddenUserIds(viewerId: UserId): Result<Set<UserId>>
}
