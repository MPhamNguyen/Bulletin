package com.jdrms.bulletin.domain.profile.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId

interface AuthRepository {
    suspend fun getCurrentUserId(): Result<UserId?>

    suspend fun getCurrentUser(): Result<StudentProfile?>
    suspend fun login(email: StudentEmail, password: String): Result<StudentProfile>
    suspend fun requestPasswordReset(email: StudentEmail): Result<Unit>
    suspend fun verifyPasswordResetCode(email: StudentEmail, code: String): Result<Unit>
    suspend fun updatePassword(password: String): Result<Unit>
    suspend fun register(
        email: StudentEmail,
        password: String,
        fullName: String,
        university: String = "CSU Long Beach"
    ): Result<StudentProfile>
    suspend fun verifyEmail(email: StudentEmail, code: String): Result<Boolean>
    suspend fun signOut(): Result<Unit>
}
