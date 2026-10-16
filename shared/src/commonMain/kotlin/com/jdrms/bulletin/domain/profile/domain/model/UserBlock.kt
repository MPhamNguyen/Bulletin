package com.jdrms.bulletin.domain.profile.domain.model

import com.jdrms.bulletin.core.common.Result

data class UserBlock(
    val blockerId: UserId,
    val blockedUserId: UserId
) {
    companion object {
        fun create(blockerId: UserId, blockedUserId: UserId): Result<UserBlock> {
            if (blockerId == blockedUserId) {
                return Result.Error(IllegalArgumentException("You cannot block your own account."))
            }
            return Result.Success(UserBlock(blockerId, blockedUserId))
        }
    }
}
