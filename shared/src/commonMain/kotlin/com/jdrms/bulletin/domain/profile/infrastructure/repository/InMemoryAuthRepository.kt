package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.generateUuid
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationCode
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationOutcome
import com.jdrms.bulletin.domain.profile.domain.model.PendingRegistration
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository

class InMemoryAuthRepository(
    private val profileRepository: ProfileRepository = InMemoryProfileRepository(),
    initialCredentials: Map<String, String> = defaultSeedCredentials,
    initialSeedUserIds: Map<String, UserId> = defaultSeedUserIds,
    // Deterministic test seam only: no email delivery, expiry, or rate-limit simulation.
    private val testVerificationCode: String? = null
) : AuthRepository {

    // Development/test adapter only. Production authentication must never retain raw passwords in application memory.
    private val credentials = initialCredentials.mapKeys { it.key.lowercase() }.toMutableMap()
    private val seedUserIds = initialSeedUserIds.mapKeys { it.key.lowercase() }
    private val profilesByEmail = mutableMapOf<String, StudentProfile>()
    private var currentUser: StudentProfile? = null
    private var pendingPasswordResetEmail: String? = null
    private var passwordResetCodeVerified = false
    private val pendingEmails = mutableSetOf<String>()

    override suspend fun getCurrentUserId(): Result<UserId?> = Result.Success(currentUser?.id)

    override suspend fun getCurrentUser(): Result<StudentProfile?> = Result.Success(currentUser)

    override suspend fun login(email: StudentEmail, password: String): Result<StudentProfile> {
        val normalizedEmail = email.value.lowercase()
        val storedPassword = credentials[normalizedEmail]
        val loginError = when {
            normalizedEmail in pendingEmails -> "Please verify your email address before logging in."
            storedPassword == null -> "Account not found. Please check your email or create an account."
            storedPassword != password -> "Incorrect password. Please try again."
            else -> null
        }
        if (loginError != null) return Result.Error(IllegalArgumentException(loginError))

        val userProfile = profilesByEmail[normalizedEmail]
            ?: seedUserIds[normalizedEmail]?.let { userId ->
                when (val result = profileRepository.getProfile(userId)) {
                    is Result.Success -> result.data?.takeIf { it.email == email }
                    is Result.Error -> null
                }
            }
        if (userProfile != null) {
            if (userProfile.isDeleted) {
                return Result.Error(
                    IllegalArgumentException("Account not found. Please check your email or create an account.")
                )
            }
            currentUser = userProfile
            return Result.Success(userProfile)
        }

        val error = IllegalArgumentException("Account not found. Please check your email or create an account.")
        return Result.Error(error)
    }

    override suspend fun requestPasswordReset(email: StudentEmail): Result<Unit> {
        if (testVerificationCode == null) {
            return Result.Error(IllegalStateException("Password reset requires a configured authentication service."))
        }
        val normalizedEmail = email.value.lowercase()
        if (!credentials.containsKey(normalizedEmail)) {
            return Result.Error(IllegalArgumentException("No account exists for that email address."))
        }
        pendingPasswordResetEmail = normalizedEmail
        passwordResetCodeVerified = false
        return Result.Success(Unit)
    }

    override suspend fun verifyPasswordResetCode(email: StudentEmail, code: String): Result<Unit> {
        if (pendingPasswordResetEmail != email.value.lowercase()) {
            return Result.Error(IllegalStateException("Request a new password reset before entering a code."))
        }
        if (code.trim() != testVerificationCode) {
            return Result.Error(IllegalArgumentException("The confirmation code is incorrect."))
        }
        passwordResetCodeVerified = true
        return Result.Success(Unit)
    }

    override suspend fun updatePassword(password: String): Result<Unit> {
        val email = pendingPasswordResetEmail
            ?: return Result.Error(IllegalStateException("Start a password reset before choosing a new password."))
        if (!passwordResetCodeVerified) {
            return Result.Error(IllegalStateException("Verify the confirmation code before choosing a new password."))
        }
        credentials[email] = password
        pendingPasswordResetEmail = null
        passwordResetCodeVerified = false
        return Result.Success(Unit)
    }

    override suspend fun register(
        email: StudentEmail,
        password: String,
        fullName: String,
        university: String
    ): Result<PendingRegistration> {
        if (testVerificationCode == null) {
            return Result.Error(
                IllegalStateException("Email verification requires a configured authentication service.")
            )
        }
        val normalizedEmail = email.value.lowercase()
        if (credentials.containsKey(normalizedEmail)) {
            return Result.Error(IllegalArgumentException("An account with this email already exists."))
        }

        val generatedId = "user_${generateUuid().take(8)}"
        val newProfile = StudentProfile(
            id = UserId(generatedId),
            email = email,
            fullName = fullName,
            university = university,
            isVerified = false
        )

        credentials[normalizedEmail] = password
        profilesByEmail[normalizedEmail] = newProfile
        pendingEmails.add(normalizedEmail)
        return Result.Success(PendingRegistration(email))
    }

    override suspend fun verifyEmail(
        email: StudentEmail,
        code: EmailVerificationCode
    ): Result<EmailVerificationOutcome> {
        if (email.value !in pendingEmails || code.value != testVerificationCode) {
            return Result.Error(IllegalArgumentException("This code is invalid or has expired."))
        }
        val profile = profilesByEmail.getValue(email.value).confirmEmail()
        profilesByEmail[email.value] = profile
        pendingEmails.remove(email.value)
        currentUser = profile
        return when (val saved = profileRepository.updateProfile(profile)) {
            is Result.Error -> Result.Success(EmailVerificationOutcome.ProfileRecoveryRequired)
            is Result.Success -> {
                currentUser = saved.data
                Result.Success(EmailVerificationOutcome.ProfileAvailable(saved.data))
            }
        }
    }

    override suspend fun resendVerificationCode(email: StudentEmail): Result<Unit> {
        return if (testVerificationCode != null && email.value in pendingEmails) {
            Result.Success(Unit)
        } else {
            Result.Error(IllegalStateException("No email verification is pending."))
        }
    }

    override suspend fun signOut(): Result<Unit> {
        currentUser = null
        return Result.Success(Unit)
    }

    companion object {
        val defaultSeedCredentials = mapOf(
            "dominic.alfonso@student.csulb.edu" to "password123"
        )
        val defaultSeedUserIds = mapOf(
            "dominic.alfonso@student.csulb.edu" to UserId(InMemoryProfileRepository.SEED_CURRENT_STUDENT_ID)
        )
    }
}
