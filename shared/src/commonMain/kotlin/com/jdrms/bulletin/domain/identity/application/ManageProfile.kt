package com.jdrms.bulletin.domain.identity.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.identity.domain.model.Profile
import com.jdrms.bulletin.domain.identity.domain.model.UserId
import com.jdrms.bulletin.domain.identity.domain.repository.ProfileRepository
import com.jdrms.bulletin.domain.identity.domain.service.IdentityValidationPolicy

class ManageProfile(
    private val profileRepository: ProfileRepository,
    private val validationPolicy: IdentityValidationPolicy = IdentityValidationPolicy()
) {
    suspend fun getProfile(id: UserId): Profile? {
        return profileRepository.getProfile(id)
    }

    suspend fun updateProfile(profile: Profile): Result<Profile> {
        val validation = validationPolicy.validateProfileUpdate(profile)
        if (validation.isError()) {
            return Result.Error((validation as Result.Error).exception)
        }
        return profileRepository.updateProfile(profile)
    }

    suspend fun searchProfiles(query: String): List<Profile> {
        if (query.isBlank()) return emptyList()
        return profileRepository.searchProfiles(query)
    }
}
