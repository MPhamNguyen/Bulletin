package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationCode
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationOutcome
import com.jdrms.bulletin.domain.profile.domain.model.PendingRegistration
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
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewInsertDto
import com.jdrms.bulletin.domain.profile.infrastructure.mapper.ProfileMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseProfileRepository(
    private val supabase: SupabaseClient,
    private val policy: ProfileValidationPolicy = ProfileValidationPolicy()
) : ProfileRepository {

    override suspend fun getProfile(userId: UserId): Result<StudentProfile?> {
        return runCatching {
            val resolvedId = resolveUserId(userId) ?: return@runCatching null

            val dto = supabase.from(PROFILES_TABLE).select {
                filter {
                    eq("id", resolvedId)
                }
            }.decodeSingleOrNull<ProfileDto>()

            dto?.let {
                val reputation = getReputation(UserId(resolvedId))
                ProfileMapper.toDomain(it, reputation)
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapProfileErrorMessage(it), it)) }
        )
    }

    override suspend fun updateProfile(profile: StudentProfile): Result<StudentProfile> {
        return runCatching {
            val resolvedId = resolveUserId(profile.id) ?: profile.id.value
            val profileToSave = if (resolvedId != profile.id.value) {
                profile.copy(id = UserId(resolvedId))
            } else {
                profile
            }
            val fullDto = ProfileMapper.toDto(profileToSave)

            val existingById = runCatching {
                supabase.from(PROFILES_TABLE).select {
                    filter {
                        eq("id", resolvedId)
                    }
                }.decodeSingleOrNull<ProfileDto>()
            }.getOrNull()

            val existingByEmail = if (existingById == null && profile.email.value.isNotBlank()) {
                runCatching {
                    supabase.from(PROFILES_TABLE).select {
                        filter {
                            eq("email", profile.email.value)
                        }
                    }.decodeSingleOrNull<ProfileDto>()
                }.getOrNull()
            } else {
                null
            }

            when {
                existingById != null -> {
                    supabase.from(PROFILES_TABLE).update(fullDto) {
                        filter {
                            eq("id", resolvedId)
                        }
                    }
                }
                existingByEmail != null -> {
                    supabase.from(PROFILES_TABLE).update(fullDto) {
                        filter {
                            eq("email", profile.email.value)
                        }
                    }
                }
                else -> {
                    supabase.from(PROFILES_TABLE).upsert(fullDto)
                }
            }
            profileToSave
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapProfileErrorMessage(it), it)) }
        )
    }

    override suspend fun submitReview(targetUserId: UserId, review: StudentReview): Result<Unit> {
        return runCatching {
            val currentUserId = supabase.auth.currentUserOrNull()?.id
                ?: error("You must be logged in to submit a review.")
            val resolvedTargetId = resolveUserId(targetUserId) ?: targetUserId.value

            val insertDto = ReviewInsertDto(
                reviewerId = currentUserId,
                revieweeId = resolvedTargetId,
                score = review.rating.score,
                comment = review.comment
            )
            supabase.from(REVIEWS_TABLE).insert(insertDto)
            Unit
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapProfileErrorMessage(it), it)) }
        )
    }

    override suspend fun deleteProfile(userId: UserId, deletedAtMillis: Long): Result<Unit> {
        return runCatching {
            val resolvedId = resolveUserId(userId) ?: userId.value
            val deleteAtIso = kotlin.time.Instant.fromEpochMilliseconds(deletedAtMillis).toString()
            val updatePayload = buildJsonObject {
                put(DELETED_AT_COLUMN, deleteAtIso)
            }
            val updatedProfiles = supabase.from(PROFILES_TABLE).update(updatePayload) {
                filter {
                    eq("id", resolvedId)
                }
                select()
            }
                .decodeList<ProfileDto>()
            requireAffectedProfileRows(updatedProfiles)
            Unit
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapProfileErrorMessage(it), it)) }
        )
    }

    override suspend fun getReputation(userId: UserId): StudentReputation {
        val resolvedId = resolveUserId(userId) ?: return policy.calculateReputation(userId, emptyList())

        val reviews = runCatching {
            val dtos = runCatching {
                supabase.from(REVIEWS_VIEW).select {
                    filter {
                        eq("reviewee_id", resolvedId)
                    }
                }.decodeList<ReviewDto>()
            }.getOrElse {
                supabase.from(REVIEWS_TABLE).select {
                    filter {
                        eq("reviewee_id", resolvedId)
                    }
                }.decodeList<ReviewDto>()
            }
            dtos.map { ProfileMapper.toDomain(it) }
        }.getOrDefault(emptyList())

        return policy.calculateReputation(UserId(resolvedId), reviews)
    }

    private fun resolveUserId(userId: UserId): String? {
        return if (userId.value == "current_student") {
            supabase.auth.currentUserOrNull()?.id
        } else if (isValidUuid(userId.value)) {
            userId.value
        } else {
            null
        }
    }

    companion object {
        const val PROFILES_TABLE = "profiles"
        const val REVIEWS_TABLE = "reviews"
        const val REVIEWS_VIEW = "reviews_with_names"
        const val DELETED_AT_COLUMN = "deleted_at"

        private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

        fun isValidUuid(value: String): Boolean = UUID_REGEX.matches(value)

        internal fun requireAffectedProfileRows(updatedProfiles: List<ProfileDto>) {
            check(updatedProfiles.isNotEmpty()) {
                "Profile was not found or could not be updated."
            }
        }

        private val PROFILE_ERROR_RULES = listOf(
            listOf("could not find the table", "schema cache") to
                "Database table not found. Please verify your Supabase schema setup.",
            listOf("unable to resolve host", "failed to connect", "timeout", "request timeout") to
                "Unable to connect to server. Please check your internet connection.",
            listOf("jwt", "unauthorized", "invalid api key", "no api key") to
                "Unauthorized database request. Please check your Supabase API credentials.",
            listOf("row-level security", "rls") to
                "Database permission denied. Please check your Supabase RLS policies.",
            listOf("invalid input syntax for type uuid", "22p02") to
                "Invalid user identifier format."
        )

        fun mapProfileErrorMessage(throwable: Throwable): String {
            val message = throwable.message ?: return "An unexpected profile error occurred."
            val lower = message.lowercase()

            for ((patterns, mappedMessage) in PROFILE_ERROR_RULES) {
                if (patterns.any { lower.contains(it) }) {
                    return mappedMessage
                }
            }

            val firstLine = message.lines().firstOrNull { it.isNotBlank() }?.trim() ?: "Profile request failed."
            val isTechnicalDump = firstLine.startsWith("url:", ignoreCase = true) ||
                firstLine.startsWith("headers:", ignoreCase = true) ||
                firstLine.startsWith("http method:", ignoreCase = true)

            return if (isTechnicalDump) {
                "Profile request failed. Please check your connection and try again."
            } else {
                firstLine
            }
        }
    }
}

class SupabaseAuthRepository(
    private val supabase: SupabaseClient,
    private val profileRepository: ProfileRepository
) : AuthRepository {

    override suspend fun getCurrentUserId(): Result<UserId?> {
        return when (val currentUser = getCurrentUser()) {
            is Result.Success -> Result.Success(currentUser.data?.id)
            is Result.Error -> currentUser
        }
    }

    override suspend fun getCurrentUser(): Result<StudentProfile?> {
        return runCatching {
            supabase.auth.awaitInitialization()
            val currentUser = supabase.auth.currentUserOrNull()
                ?.takeIf { it.emailConfirmedAt != null } ?: return@runCatching null
            val userId = UserId(currentUser.id)

            when (val profileResult = profileRepository.getProfile(userId)) {
                is Result.Success -> {
                    val profile = profileResult.data ?: return@runCatching null
                    if (profile.isDeleted) {
                        rejectSession("Account has been deleted.")
                    }
                    profile
                }
                is Result.Error -> throw profileResult.exception
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapAuthErrorMessage(it), it)) }
        )
    }

    override suspend fun register(
        email: StudentEmail,
        password: String,
        fullName: String,
        university: String
    ): Result<PendingRegistration> {
        return runCatching {
            val restoredProfile = try {
                supabase.auth.signUpWith(Email) {
                    this.email = email.value
                    this.password = password
                    data = buildJsonObject {
                        put("full_name", fullName)
                        put("university", university)
                    }
                }
                null
            } catch (throwable: AuthRestException) {
                if (!isExistingUserError(throwable)) throw throwable

                supabase.auth.signInWith(Email) {
                    this.email = email.value
                    this.password = password
                }
                val currentUser = supabase.auth.currentUserOrNull()
                    ?: error("Failed to retrieve authenticated user session.")
                if (currentUser.emailConfirmedAt == null) {
                    rejectSession("Please verify your email address before restoring the account.")
                }
                val existingProfile = when (val result = profileRepository.getProfile(UserId(currentUser.id))) {
                    is Result.Success -> result.data
                    is Result.Error -> throw result.exception
                } ?: error("Profile record not found. Please contact support.")
                val restored = when (val result = existingProfile.restoreForRegistration(fullName, university)) {
                    is Result.Success -> result.data
                    is Result.Error -> throw result.exception
                }
                when (val result = profileRepository.updateProfile(restored)) {
                    is Result.Success -> result.data
                    is Result.Error -> throw result.exception
                }
            }
            // Confirmation must be enforced by the server, never inferred from a profile row.
            if (restoredProfile == null && supabase.auth.currentSessionOrNull() != null) {
                rejectSession("Email confirmation is unavailable. Please contact support.")
            }
            PendingRegistration(email, restoredProfile)
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { authFailure(it) }
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
            if (currentUser.emailConfirmedAt == null) {
                rejectSession("Please verify your email address before logging in.")
            }

            val userId = UserId(currentUser.id)

            val profileResult = profileRepository.getProfile(userId)
            val profile = when (profileResult) {
                is Result.Success -> {
                    val foundProfile = profileResult.data
                        ?: rejectSession("Profile record not found. Please contact support.")
                    if (foundProfile.isDeleted) {
                        rejectSession("Account has been deleted.")
                    }
                    foundProfile
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

    override suspend fun verifyEmail(
        email: StudentEmail,
        code: EmailVerificationCode
    ): Result<EmailVerificationOutcome> {
        return runCatching {
            supabase.auth.verifyEmailOtp(
                type = OtpType.Email.EMAIL,
                email = email.value,
                token = code.value
            )
            val user = supabase.auth.currentUserOrNull()
                ?: error("Email verification did not create a session. Please try again.")
            if (user.emailConfirmedAt == null || user.email?.lowercase() != email.value) {
                rejectSession("Email verification failed. Please try again.")
            }
            when (val result = getCurrentUser()) {
                is Result.Success -> result.data?.let(EmailVerificationOutcome::ProfileAvailable)
                    ?: EmailVerificationOutcome.ProfileRecoveryRequired
                is Result.Error -> EmailVerificationOutcome.ProfileRecoveryRequired
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { authFailure(it) }
        )
    }

    override suspend fun resendVerificationCode(email: StudentEmail): Result<Unit> {
        return runCatching {
            supabase.auth.resendEmail(OtpType.Email.SIGNUP, email.value)
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { authFailure(it) }
        )
    }

    private fun authFailure(throwable: Throwable): Result.Error {
        if (throwable is CancellationException) throw throwable
        return Result.Error(Exception(mapAuthErrorMessage(throwable), throwable))
    }

    private suspend fun rejectSession(message: String): Nothing {
        try {
            supabase.auth.signOut()
        } finally {
            // Rejected credentials must not survive a failed remote sign-out or an app restart.
            supabase.auth.clearSession()
        }
        error(message)
    }

    override suspend fun signOut(): Result<Unit> {
        return runCatching {
            supabase.auth.signOut()
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(Exception(mapAuthErrorMessage(it), it)) }
        )
    }

    companion object {
        private val COMMON_ERROR_RULES = listOf(
            listOf("otp_expired", "token has expired", "token is invalid") to
                "This code is invalid or has expired. Request a new code and try again.",
            listOf("over_request_rate_limit", "over_email_send_rate_limit", "email rate limit exceeded") to
                "Too many signup attempts. Please wait a few minutes before trying again.",
            listOf("user_already_exists", "user already registered") to
                "An account with this email address already exists. Please log in instead.",
            listOf("invalid_credentials", "invalid login credentials") to
                "Invalid email or password. Please try again.",
            listOf("email_address_invalid", "invalid email") to
                "Invalid email address. Please use a valid university or personal email domain.",
            listOf("bulletin requires a valid .edu university email") to
                "Bulletin requires a valid .edu university email.",
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
            "http",
            "header",
            "url:",
            "rest/v1",
            "supabase.co",
            "select=",
            "apikey",
            "syntax"
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

        internal fun isExistingUserError(throwable: Throwable): Boolean {
            val message = throwable.message?.lowercase() ?: return false
            return message.contains("user_already_exists") || message.contains("user already registered")
        }
    }
}
