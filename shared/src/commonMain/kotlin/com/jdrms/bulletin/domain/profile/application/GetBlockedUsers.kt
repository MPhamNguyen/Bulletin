package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import com.jdrms.bulletin.domain.profile.domain.repository.UserBlockRepository

data class BlockedUser(
    val id: UserId,
    val profile: StudentProfile?
)

class GetBlockedUsers(
    private val authRepository: AuthRepository,
    private val blockRepository: UserBlockRepository,
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(): Result<List<BlockedUser>> {
        val viewerId = when (val result = authRepository.getCurrentUserId()) {
            is Result.Success -> result.data
            is Result.Error -> return result
        } ?: return Result.Error(IllegalStateException("Sign in to view blocked users."))

        val blockedIds = when (val result = blockRepository.getBlockedUserIds(viewerId)) {
            is Result.Success -> result.data
            is Result.Error -> return result
        }
        return loadProfiles(blockedIds)
    }

    private suspend fun loadProfiles(blockedIds: Set<UserId>): Result<List<BlockedUser>> {
        val users = blockedIds.mapNotNull { blockedId ->
            when (val result = profileRepository.getProfile(blockedId)) {
                is Result.Success -> BlockedUser(blockedId, result.data)
                is Result.Error -> return result
            }
        }.sortedBy { it.profile?.fullName.orEmpty().lowercase() }
        return Result.Success(users)
    }
}
