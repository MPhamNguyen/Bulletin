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

    override suspend fun getProfile(userId: UserId): Result<StudentProfile?> {
        return runCatching {
            val resolvedUserId = if (userId.value == "current_student") {
                supabase.auth.currentUserOrNull()?.id
            } else {
                userId.value
            }

            if (resolvedUserId == null) {
                return@runCatching null
            }

            val dto = supabase.from(PROFILES_TABLE).select {
                filter {
                    eq("id", resolvedUserId)
                }
            }.decodeSingleOrNull<ProfileDto>()

            dto?.let {
                val reputation = getReputation(UserId(resolvedUserId))
                ProfileMapper.toDomain(it, reputation)
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { error ->
                val msg = error.message?.lowercase().orEmpty()
                if (msg.contains("invalid input syntax for type uuid") || msg.contains("22p02")) {
                    Result.Success(null)
                } else {
                    Result.Error(Exception(mapProfileErrorMessage(error), error))
                }
            }
        )
    }

    override suspend fun updateProfile(profile: StudentProfile): Result<StudentProfile> {
        return runCatching {
            val dto = ProfileMapper.toDto(profile)
            supabase.from(PROFILES_TABLE).upsert(dto)
            profile
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapProfileErrorMessage(it), it)) }
        )
    }

    override suspend fun submitReview(targetUserId: UserId, review: StudentReview): Result<Unit> {
        return runCatching {
            val dto = ProfileMapper.toDto(review)
            supabase.from(REVIEWS_TABLE).insert(dto)
            Unit
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapProfileErrorMessage(it), it)) }
        )
    }

    override suspend fun getReputation(userId: UserId): StudentReputation {
        val resolvedUserId = if (userId.value == "current_student") {
            supabase.auth.currentUserOrNull()?.id ?: userId.value
        } else {
            userId.value
        }

        val reviews = runCatching {
            val dtos = supabase.from(REVIEWS_TABLE).select {
                filter {
                    eq("reviewee_id", resolvedUserId)
                }
            }.decodeList<ReviewDto>()
            dtos.map { ProfileMapper.toDomain(it) }
        }.getOrDefault(emptyList())

        return policy.calculateReputation(UserId(resolvedUserId), reviews)
    }

    companion object {
        const val PROFILES_TABLE = "profiles"
        const val REVIEWS_TABLE = "reviews"

        fun mapProfileErrorMessage(throwable: Throwable): String {
            return SupabaseAuthRepository.mapErrorMessage(
                throwable = throwable,
                defaultMessage = "Unable to complete profile operation. Please try again."
            )
        }
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
            val generatedFallbackId = "user_${email.value.hashCode().toUInt() and 0x7FFFFFFFu}"
            val userId = UserId(resolvedUserId ?: generatedFallbackId)

            val newProfile = StudentProfile(
                id = userId,
                email = email,
                fullName = fullName,
                university = university,
                isVerified = false
            )

            if (resolvedUserId != null) {
                val updateResult = profileRepository.updateProfile(newProfile)
                if (updateResult is Result.Error) {
                    throw updateResult.exception
                }
            }

            newProfile
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapAuthErrorMessage(it), it)) }
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
            val metadataName = (currentUser.userMetadata?.get("full_name") as? JsonPrimitive)?.content?.takeIf {
                it.isNotBlank()
            }
            val metadataUniversity = (currentUser.userMetadata?.get("university") as? JsonPrimitive)?.content?.takeIf {
                it.isNotBlank()
            }

            val profileResult = profileRepository.getProfile(userId)
            val profile = when (profileResult) {
                is Result.Success -> {
                    if (profileResult.data != null) {
                        profileResult.data
                    } else {
                        // Profile row does not exist in DB yet, create it with user metadata
                        val newProfile = StudentProfile(
                            id = userId,
                            email = email,
                            fullName = metadataName ?: "Student",
                            university = metadataUniversity ?: "CSU Long Beach",
                            isVerified = currentUser.emailConfirmedAt != null
                        )
                        val saveResult = profileRepository.updateProfile(newProfile)
                        if (saveResult is Result.Error) {
                            throw saveResult.exception
                        }
                        newProfile
                    }
                }
                is Result.Error -> {
                    // Propagate repository errors instead of silently overwriting existing profile data
                    throw profileResult.exception
                }
            }

            profile
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapAuthErrorMessage(it), it)) }
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
            onFailure = { Result.Error(Exception(mapAuthErrorMessage(it), it)) }
        )
    }

    companion object {
        private val COMMON_ERROR_RULES = listOf(
            listOf("user_already_exists", "user already registered") to
                "An account with this email address already exists. Please log in instead.",
            listOf("over_email_send_rate_limit", "email rate limit exceeded") to
                "Too many signup attempts. Please wait a few minutes before trying again.",
            listOf("invalid_credentials", "invalid login credentials") to
                "Invalid email or password. Please try again.",
            listOf("email_address_invalid", "invalid email") to
                "Invalid email address. Please use a valid university or personal email domain.",
            listOf("signup_disabled", "signups not allowed") to
                "Account registration is currently disabled.",
            listOf("email_not_confirmed") to
                "Please verify your email address before logging in.",
            listOf("invalid input syntax for type uuid", "invalid input syntax", "22p02") to
                "The requested user account was not found.",
            listOf("jwt expired", "invalid jwt", "pgrst301", "auth_token_expired") to
                "Your session has expired. Please log in again.",
            listOf("row-level security", "permission denied", "42501") to
                "You do not have permission to perform this action.",
            listOf("unable to resolve host", "failed to connect", "timeout", "connectexception", "sockettimeout") to
                "Unable to connect to server. Please check your internet connection."
        )

        private val TECHNICAL_PATTERNS = listOf(
            "http", "header", "url:", "rest/v1", "supabase.co", "select=", "apikey", "syntax", "headers:"
        )

        fun mapErrorMessage(
            throwable: Throwable,
            defaultMessage: String = "An unexpected error occurred."
        ): String {
            val message = throwable.message ?: return defaultMessage
            val lower = message.lowercase()

            for ((patterns, mappedMessage) in COMMON_ERROR_RULES) {
                if (patterns.any { lower.contains(it) }) {
                    return mappedMessage
                }
            }

            val firstLine = message.lines().firstOrNull()?.trim() ?: defaultMessage
            val isTechnicalDump = TECHNICAL_PATTERNS.any { lower.contains(it) }

            return if (isTechnicalDump) {
                defaultMessage
            } else {
                firstLine
            }
        }

        fun mapAuthErrorMessage(throwable: Throwable): String {
            return mapErrorMessage(
                throwable = throwable,
                defaultMessage = "Authentication failed. Please check your details and try again."
            )
        }
    }
}
