package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.ConversationUserBlocker
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.profile.application.BlockUser
import com.jdrms.bulletin.domain.profile.application.UnblockUser
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.UserBlockRepository

/** Translates messages identities at the boundary; profile owns the block invariant and persistence. */
internal class ProfileConversationUserBlocker(
    private val auth: AuthRepository,
    private val repository: UserBlockRepository,
    private val blockUser: BlockUser,
    private val unblockUser: UnblockUser,
    private val listingChangedSignal: RefreshSignal
) : ConversationUserBlocker {
    override suspend fun isBlocked(userId: SenderId): Result<Boolean> {
        val viewer = when (val result = auth.getCurrentUserId()) {
            is Result.Success -> result.data
            is Result.Error -> return result
        } ?: return Result.Error(IllegalStateException("Sign in to view blocked users."))
        return when (val result = repository.getBlockedUserIds(viewer)) {
            is Result.Success -> Result.Success(UserId(userId.value) in result.data)
            is Result.Error -> result
        }
    }

    override suspend fun block(userId: SenderId): Result<Unit> =
        blockUser(UserId(userId.value)).also { if (it is Result.Success) listingChangedSignal.emit() }

    override suspend fun unblock(userId: SenderId): Result<Unit> =
        unblockUser(UserId(userId.value)).also { if (it is Result.Success) listingChangedSignal.emit() }
}
