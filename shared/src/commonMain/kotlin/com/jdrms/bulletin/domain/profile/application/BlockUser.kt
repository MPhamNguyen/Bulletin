package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.UserBlock
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.UserBlockRepository

class BlockUser(
    private val authRepository: AuthRepository,
    private val blockRepository: UserBlockRepository
) {
    suspend operator fun invoke(blockedUserId: UserId): Result<Unit> {
        val blockerId = when (val result = authRepository.getCurrentUserId()) {
            is Result.Success -> result.data ?: return Result.Error(IllegalStateException("Sign in to block a user."))
            is Result.Error -> return result
        }
        val block = when (val result = UserBlock.create(blockerId, blockedUserId)) {
            is Result.Success -> result.data
            is Result.Error -> return result
        }
        return blockRepository.block(block)
    }
}

class UnblockUser(
    private val authRepository: AuthRepository,
    private val blockRepository: UserBlockRepository
) {
    suspend operator fun invoke(blockedUserId: UserId): Result<Unit> {
        val blockerId = when (val result = authRepository.getCurrentUserId()) {
            is Result.Success -> result.data
            is Result.Error -> return result
        } ?: return Result.Error(IllegalStateException("Sign in to unblock a user."))

        return when (val block = UserBlock.create(blockerId, blockedUserId)) {
            is Result.Success -> blockRepository.unblock(block.data)
            is Result.Error -> block
        }
    }
}
