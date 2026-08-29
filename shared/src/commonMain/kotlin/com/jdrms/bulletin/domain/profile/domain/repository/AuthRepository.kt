package com.jdrms.bulletin.domain.profile.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile

interface AuthRepository {
    suspend fun login(email: StudentEmail, password: String): Result<StudentProfile>
    suspend fun register(
        email: StudentEmail,
        password: String,
        fullName: String,
        university: String = "CSU Long Beach"
    ): Result<StudentProfile>
    suspend fun verifyEmail(email: StudentEmail, code: String): Result<Boolean>
}
