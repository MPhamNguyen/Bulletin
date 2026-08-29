package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ProfileDto
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewDto
import com.jdrms.bulletin.domain.profile.infrastructure.mapper.ProfileMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseProfileRepository(
    private val supabase: SupabaseClient,
    private val policy: ProfileValidationPolicy = ProfileValidationPolicy()
) : ProfileRepository {

    override suspend fun getProfile(userId: UserId): StudentProfile? {
        return runCatching {
            val dto = supabase.from(PROFILES_TABLE).select {
                filter {
                    eq("id", userId.value)
                }
            }.decodeSingleOrNull<ProfileDto>()

            dto?.let {
                val reputation = getReputation(userId)
                ProfileMapper.toDomain(it, reputation)
            }
        }.getOrNull()
    }

    override suspend fun updateProfile(profile: StudentProfile): Result<StudentProfile> {
        return runCatching {
            val dto = ProfileMapper.toDto(profile)
            supabase.from(PROFILES_TABLE).upsert(dto)
            profile
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it) }
        )
    }

    override suspend fun submitReview(targetUserId: UserId, review: StudentReview): Result<Unit> {
        return runCatching {
            val dto = ProfileMapper.toDto(review)
            supabase.from(REVIEWS_TABLE).insert(dto)
            Unit
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it) }
        )
    }

    override suspend fun getReputation(userId: UserId): StudentReputation {
        val reviews = runCatching {
            val dtos = supabase.from(REVIEWS_TABLE).select {
                filter {
                    eq("reviewee_id", userId.value)
                }
            }.decodeList<ReviewDto>()
            dtos.map { ProfileMapper.toDomain(it) }
        }.getOrDefault(emptyList())

        return policy.calculateReputation(userId, reviews)
    }

    companion object {
        const val PROFILES_TABLE = "profiles"
        const val REVIEWS_TABLE = "reviews"
    }
}

class SupabaseAuthRepository(
    private val supabase: SupabaseClient,
    private val profileRepository: ProfileRepository
) : AuthRepository {

    override suspend fun register(
        email: StudentEmail,
        password: String,
        fullName: String,
        university: String
    ): Result<StudentProfile> {
        return runCatching {
            val authUser = supabase.auth.signUpWith(Email) {
                this.email = email.value
                this.password = password
                data = buildJsonObject {
                    put("full_name", fullName)
                    put("university", university)
                }
            }

            val currentUser = supabase.auth.currentUserOrNull()
            val resolvedUserId = authUser?.id ?: currentUser?.id
            val userId = UserId(resolvedUserId ?: "pending_confirmation")

            val newProfile = StudentProfile(
                id = userId,
                email = email,
                fullName = fullName,
                university = university,
                isVerified = false
            )

            if (resolvedUserId != null) {
                runCatching {
                    profileRepository.updateProfile(newProfile)
                }
            }

            newProfile
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it) }
        )
    }

    override suspend fun login(email: StudentEmail, password: String): Result<StudentProfile> {
        return runCatching {
            supabase.auth.signInWith(Email) {
                this.email = email.value
                this.password = password
            }

            val currentUser = supabase.auth.currentUserOrNull()
                ?: error("Failed to retrieve authenticated user session.")

            val userId = UserId(currentUser.id)
            val metadataName = (currentUser.userMetadata?.get("full_name") as? JsonPrimitive)?.content
            val metadataUniversity = (currentUser.userMetadata?.get("university") as? JsonPrimitive)?.content

            val existingProfile = profileRepository.getProfile(userId)
            val profile = if (existingProfile != null) {
                existingProfile
            } else {
                val newProfile = StudentProfile(
                    id = userId,
                    email = email,
                    fullName = metadataName ?: "Student",
                    university = metadataUniversity ?: "CSU Long Beach",
                    isVerified = currentUser.emailConfirmedAt != null
                )
                runCatching { profileRepository.updateProfile(newProfile) }
                newProfile
            }

            profile
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it) }
        )
    }

    override suspend fun verifyEmail(email: StudentEmail, code: String): Result<Boolean> {
        return runCatching {
            supabase.auth.verifyEmailOtp(
                type = OtpType.Email.EMAIL,
                email = email.value,
                token = code
            )
            true
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it) }
        )
    }
}
