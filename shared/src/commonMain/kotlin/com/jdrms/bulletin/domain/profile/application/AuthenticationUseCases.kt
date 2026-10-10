package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.PendingRegistration
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy

class SignInUser(
    private val authRepository: AuthRepository,
    private val policy: ProfileValidationPolicy = ProfileValidationPolicy()
) {
    suspend operator fun invoke(email: String, password: String): Result<StudentProfile> {
        val address = email.trim()
        return when (val validation = policy.validateLogin(address, password)) {
            is Result.Success -> authRepository.login(StudentEmail(address), password)
            is Result.Error -> validation
        }
    }
}

class RegisterStudent(
    private val authRepository: AuthRepository,
    private val policy: ProfileValidationPolicy = ProfileValidationPolicy()
) {
    suspend operator fun invoke(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        university: String = ""
    ): Result<PendingRegistration> {
        val first = firstName.trim()
        val last = lastName.trim()
        val address = email.trim()
        return when (val validation = policy.validateRegistration(first, last, address, password)) {
            is Result.Success -> authRepository.register(StudentEmail(address), password, "$first $last", university)
            is Result.Error -> validation
        }
    }
}
