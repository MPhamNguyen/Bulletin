package com.jdrms.bulletin.domain.profile.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationCode
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationOutcome
import com.jdrms.bulletin.domain.profile.domain.model.PendingRegistration
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId

interface AuthRepository {
    /** Only identities confirmed by the authentication provider may be restored. */
    suspend fun getCurrentUserId(): Result<UserId?>

    suspend fun getCurrentUser(): Result<StudentProfile?>
    suspend fun login(email: StudentEmail, password: String): Result<StudentProfile>

    /** Requests email confirmation; success must not establish an authenticated session. */
    suspend fun register(
        email: StudentEmail,
        password: String,
        fullName: String,
        university: String = "CSU Long Beach"
    ): Result<PendingRegistration>

    /** Exchanges a valid code for a confirmed session, even when profile loading needs a separate retry. */
    suspend fun verifyEmail(email: StudentEmail, code: EmailVerificationCode): Result<EmailVerificationOutcome>

    suspend fun resendVerificationCode(email: StudentEmail): Result<Unit>
    suspend fun signOut(): Result<Unit>
}
