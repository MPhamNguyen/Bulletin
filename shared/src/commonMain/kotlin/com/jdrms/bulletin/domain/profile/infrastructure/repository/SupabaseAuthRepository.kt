package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationCode
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationOutcome
import com.jdrms.bulletin.domain.profile.domain.model.PendingRegistration
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseAuthRepository(
    private val supabase: SupabaseClient,
    private val profileRepository: ProfileRepository,
    private val externalBrowser: ExternalAuthBrowser? = null,
    private val onSessionEnded: suspend () -> Unit = {}
) : AuthRepository {

    private val externalSignIn = externalBrowser?.let {
        SupabaseExternalSignIn(supabase, it, ::getCurrentUser) {
            supabase.auth.clearSession()
            onSessionEnded()
        }
    }

    override suspend fun signInExternally() = externalSignIn?.signIn() ?: super.signInExternally()

    private var passwordResetRequested = false
    private var passwordResetCodeVerified = false

    override suspend fun getCurrentUserId(): Result<UserId?> {
        return runCatching {
            supabase.auth.awaitInitialization()
            val currentUser = supabase.auth.currentUserOrNull()
                ?.takeIf { it.emailConfirmedAt != null } ?: return@runCatching null
            enforceExternalIdentity(currentUser)
            val userId = UserId(currentUser.id)

            when (val profileResult = profileRepository.getProfile(userId)) {
                is Result.Success -> if (profileResult.data?.isDeleted == true) null else userId
                is Result.Error -> throw profileResult.exception
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapAuthErrorMessage(it), it)) }
        )
    }

    override suspend fun getCurrentUser(): Result<StudentProfile?> {
        return runCatching {
            supabase.auth.awaitInitialization()
            val currentUser = supabase.auth.currentUserOrNull()
                ?.takeIf { it.emailConfirmedAt != null } ?: return@runCatching null
            enforceExternalIdentity(currentUser)
            val profile = readOrCreateProfile(currentUser)
            if (profile.isDeleted) null else profile
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
            supabase.auth.signUpWith(Email) {
                this.email = email.value
                this.password = password
                data = buildJsonObject {
                    put("full_name", fullName)
                    put("university", university)
                }
            }
            // Confirmation must be enforced by the server, never inferred from a profile row.
            if (supabase.auth.currentSessionOrNull() != null) {
                rejectSession("Email confirmation is unavailable. Please contact support.")
            }
            PendingRegistration(email)
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { authFailure(it) }
        )
    }

    override suspend fun login(email: StudentEmail, password: String): Result<StudentProfile> {
        externalBrowser?.clearRecovered()
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

            readOrCreateProfile(currentUser, email).also { profile ->
                if (profile.isDeleted) rejectSession("Account not found. Please check your email or create an account.")
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapAuthErrorMessage(it), it)) }
        )
    }

    override suspend fun requestPasswordReset(email: StudentEmail): Result<Unit> {
        return runCatching {
            supabase.auth.resetPasswordForEmail(email = email.value)
            passwordResetRequested = true
            passwordResetCodeVerified = false
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(Exception(mapAuthErrorMessage(it), it)) }
        )
    }

    override suspend fun verifyPasswordResetCode(email: StudentEmail, code: String): Result<Unit> {
        return if (!passwordResetRequested) {
            Result.Error(IllegalStateException("Request a new password reset before entering a code."))
        } else {
            runCatching {
                supabase.auth.verifyEmailOtp(
                    type = OtpType.Email.RECOVERY,
                    email = email.value,
                    token = code.trim()
                )
                passwordResetCodeVerified = true
            }.fold(
                onSuccess = { Result.Success(Unit) },
                onFailure = { Result.Error(Exception(mapAuthErrorMessage(it), it)) }
            )
        }
    }

    override suspend fun updatePassword(password: String): Result<Unit> {
        if (!passwordResetCodeVerified) {
            return Result.Error(IllegalStateException("Verify the confirmation code before choosing a new password."))
        }
        return runCatching {
            supabase.auth.updateUser {
                this.password = password
            }
            passwordResetRequested = false
            passwordResetCodeVerified = false
        }.fold(
            onSuccess = { Result.Success(Unit) },
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

    private suspend fun enforceExternalIdentity(user: io.github.jan.supabase.auth.user.UserInfo) {
        if (user.appMetadata?.get("provider") == JsonPrimitive("azure")) {
            if (MicrosoftIdentityPolicy.rejection(user) != null) {
                rejectSession("Microsoft account verification failed. Please sign in again.")
            }
        }
    }

    private suspend fun rejectSession(message: String): Nothing {
        try {
            supabase.auth.signOut()
        } finally {
            // Rejected credentials must not survive a failed remote sign-out or an app restart.
            supabase.auth.clearSession()
            onSessionEnded()
        }
        error(message)
    }

    override suspend fun signOut(): Result<Unit> {
        externalBrowser?.clearRecovered()
        return runCatching {
            try {
                supabase.auth.signOut()
            } finally {
                supabase.auth.clearSession()
                onSessionEnded()
            }
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(Exception(mapAuthErrorMessage(it), it)) }
        )
    }

    private suspend fun readOrCreateProfile(
        user: io.github.jan.supabase.auth.user.UserInfo,
        fallbackEmail: StudentEmail? = null
    ): StudentProfile = when (val result = profileRepository.getProfile(UserId(user.id))) {
        is Result.Success -> result.data ?: createProfileFromAuthUser(user, fallbackEmail)
        is Result.Error -> throw result.exception
    }

    private suspend fun createProfileFromAuthUser(
        authUser: io.github.jan.supabase.auth.user.UserInfo,
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
    }
}
