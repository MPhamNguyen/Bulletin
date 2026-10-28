package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository

data class ProfileOverview(
    val profile: StudentProfile,
    val reputation: StudentReputation,
    val activeListingsCount: Int
)

class GetProfileOverview(
    private val profileRepository: ProfileRepository,
    private val activeListingsProvider: ProfileActiveListingsProvider
) {
    suspend operator fun invoke(userId: UserId): Result<ProfileOverview?> = load(userId, refresh = false)

    suspend fun refresh(userId: UserId): Result<ProfileOverview?> = load(userId, refresh = true)

    private suspend fun load(userId: UserId, refresh: Boolean): Result<ProfileOverview?> {
        val result = if (refresh) {
            profileRepository.refreshProfile(userId)
        } else {
            profileRepository.getProfile(userId)
        }
        return when (result) {
            is Result.Success -> {
                val profile = result.data
                if (profile == null) {
                    Result.Success(null)
                } else {
                    Result.Success(
                        ProfileOverview(
                            profile = profile,
                            reputation = profileRepository.getReputation(userId),
                            activeListingsCount = activeListingsProvider.getActiveListingsCount(userId)
                        )
                    )
                }
            }
            is Result.Error -> result
        }
    }
}

class GetProfileActivity(private val activeListingsProvider: ProfileActiveListingsProvider) {
    suspend operator fun invoke(userId: UserId): Int = activeListingsProvider.getActiveListingsCount(userId)
}
