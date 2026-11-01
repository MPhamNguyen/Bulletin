package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.serialization.json.JsonPrimitive

/** Reads the profile for an authenticated user, creating it from auth metadata when none exists yet. */
internal class SupabaseAuthProfileProvisioner(
    private val profileRepository: ProfileRepository
) {
    suspend fun readOrCreate(
        user: UserInfo,
        fallbackEmail: StudentEmail? = null
    ): StudentProfile = when (val result = profileRepository.getProfile(UserId(user.id))) {
        is Result.Success -> result.data ?: createFromAuthUser(user, fallbackEmail)
        is Result.Error -> throw result.exception
    }

    private suspend fun createFromAuthUser(
        authUser: UserInfo,
        fallbackEmail: StudentEmail? = null
    ): StudentProfile {
        val email = authUser.email?.let(::StudentEmail) ?: fallbackEmail
            ?: error("Authenticated user does not have an email address.")
        val metadataName = (authUser.userMetadata?.get("full_name") as? JsonPrimitive)?.content?.takeIf {
            it.isNotBlank()
        } ?: (authUser.userMetadata?.get("name") as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
        val metadataUniversity = (authUser.userMetadata?.get("university") as? JsonPrimitive)?.content?.takeIf {
            it.isNotBlank()
        }
        val profile = StudentProfile(
            id = UserId(authUser.id),
            email = email,
            fullName = metadataName ?: "Student",
            university = if (authUser.appMetadata?.get("provider") == JsonPrimitive("azure")) {
                ""
            } else {
                metadataUniversity.orEmpty()
            },
            isVerified = authUser.emailConfirmedAt != null
        )
        when (val saveResult = profileRepository.updateProfile(profile)) {
            is Result.Success -> return saveResult.data
            is Result.Error -> throw saveResult.exception
        }
    }
}
