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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseAuthRepository(
    private val supabase: SupabaseClient,
    private val profileRepository: ProfileRepository,
    private val externalBrowser: ExternalAuthBrowser? = null,
    onSessionEnded: suspend () -> Unit = {}
) : AuthRepository {

    private val sessionGuard = SupabaseAuthSessionGuard(supabase, onSessionEnded)
    private val profileProvisioner = SupabaseAuthProfileProvisioner(profileRepository)
    private val passwordRecovery = SupabasePasswordRecovery(supabase)

    private val externalSignIn = externalBrowser?.let {
        SupabaseExternalSignIn(supabase, it, ::getCurrentUser, sessionGuard::endSession)
    }

    override suspend fun signInExternally() = externalSignIn?.signIn() ?: super.signInExternally()

    override suspend fun getCurrentUserId(): Result<UserId?> {
        return runCatching {
            supabase.auth.awaitInitialization()
            val currentUser = supabase.auth.currentUserOrNull()
                ?.takeIf { it.emailConfirmedAt != null } ?: return@runCatching null
            sessionGuard.enforceExternalIdentity(currentUser)
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
            sessionGuard.enforceExternalIdentity(currentUser)
            val profile = profileProvisioner.readOrCreate(currentUser)
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
                sessionGuard.rejectSession("Email confirmation is unavailable. Please contact support.")
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
                sessionGuard.rejectSession("Please verify your email address before logging in.")
            }

            profileProvisioner.readOrCreate(currentUser, email).also { profile ->
                if (profile.isDeleted) {
                    sessionGuard.rejectSession("Account not found. Please check your email or create an account.")
                }
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapAuthErrorMessage(it), it)) }
        )
    }

    override suspend fun requestPasswordReset(email: StudentEmail): Result<Unit> = passwordRecovery.request(email)

    override suspend fun verifyPasswordResetCode(email: StudentEmail, code: String): Result<Unit> =
        passwordRecovery.verifyCode(email, code)

    override suspend fun updatePassword(password: String): Result<Unit> = passwordRecovery.updatePassword(password)

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
                sessionGuard.rejectSession("Email verification failed. Please try again.")
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

    override suspend fun signOut(): Result<Unit> {
        externalBrowser?.clearRecovered()
        return runCatching {
            sessionGuard.signOut()
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(Exception(mapAuthErrorMessage(it), it)) }
        )
    }

    private fun authFailure(throwable: Throwable): Result.Error {
        if (throwable is CancellationException) throw throwable
        return Result.Error(Exception(mapAuthErrorMessage(throwable), throwable))
    }

    companion object {
        fun mapErrorMessage(
            throwable: Throwable,
            defaultMessage: String = "An unexpected error occurred."
        ): String = SupabaseAuthErrorMapper.mapErrorMessage(throwable, defaultMessage)

        fun mapAuthErrorMessage(throwable: Throwable): String =
            SupabaseAuthErrorMapper.mapAuthErrorMessage(throwable)
    }
}
