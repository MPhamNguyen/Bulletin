package com.jdrms.bulletin.domain.profile.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationCode
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

    /** Exchanges a valid, unexpired code for a provider-confirmed session and its profile. */
    suspend fun verifyEmail(email: StudentEmail, code: EmailVerificationCode): Result<StudentProfile>

    suspend fun resendVerificationCode(email: StudentEmail): Result<Unit>
    suspend fun signOut(): Result<Unit>
}
