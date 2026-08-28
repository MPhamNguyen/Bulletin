package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.service.IdentityValidationPolicy

class VerifyStudentEmail(
    private val authRepository: AuthRepository,
    private val validationPolicy: IdentityValidationPolicy = IdentityValidationPolicy()
) {
    suspend operator fun invoke(email: String, code: String): Result<Boolean> {
        val emailValidation = validationPolicy.validateUniversityRegistration(email)
        if (emailValidation.isError()) {
            return Result.Error((emailValidation as Result.Error).exception)
        }
        if (code.isBlank() || code.length < 4) {
            return Result.Error(IllegalArgumentException("Invalid verification code."))
        }
        return authRepository.verifyEmail(email, code)
    }
}
