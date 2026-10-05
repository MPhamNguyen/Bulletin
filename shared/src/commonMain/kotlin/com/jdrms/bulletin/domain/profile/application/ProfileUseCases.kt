package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationCode
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationOutcome
import com.jdrms.bulletin.domain.profile.domain.model.PendingRegistration
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy

class AuthenticateUser(
    private val authRepository: AuthRepository,
    private val policy: ProfileValidationPolicy = ProfileValidationPolicy()
) {
    suspend fun login(email: StudentEmail, password: String): Result<StudentProfile> {
        val validation = policy.validateLogin(
            emailStr = email.value,
            password = password
        )
        if (validation.isError()) {
            return Result.Error((validation as Result.Error).exception)
        }
        val loginResult = authRepository.login(email, password)
        return when (loginResult) {
            is Result.Success -> {
                if (loginResult.data.isDeleted) {
                    authRepository.signOut()
                    Result.Error(IllegalArgumentException("Account has been deleted."))
                } else {
                    loginResult
                }
            }
            is Result.Error -> loginResult
        }
    }

    suspend fun register(
        email: StudentEmail,
        password: String,
        fullName: String,
        university: String = "CSU Long Beach"
    ): Result<PendingRegistration> {
        val validation = policy.validateRegistration(
            emailStr = email.value,
            password = password,
            fullName = fullName
        )
        if (validation.isError()) {
            return Result.Error((validation as Result.Error).exception)
        }
        return authRepository.register(email, password, fullName, university)
    }
}

class RestoreAuthenticatedProfile(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<StudentProfile?> {
        return authRepository.getCurrentUser()
    }
}

class GetAuthenticatedUserId(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<UserId?> {
        return authRepository.getCurrentUserId()
    }
}

class SignOutUser(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.signOut()
    }
}

class VerifyStudentEmail(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: StudentEmail, code: String): Result<EmailVerificationOutcome> {
        return when (val parsed = EmailVerificationCode.parse(code)) {
            is Result.Success -> authRepository.verifyEmail(email, parsed.data)
            is Result.Error -> parsed
        }
    }
}

class ResendVerificationCode(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: StudentEmail): Result<Unit> = authRepository.resendVerificationCode(email)
}

class ManageProfile(
    private val profileRepository: ProfileRepository
) {
    suspend fun getProfile(userId: UserId): Result<StudentProfile?> {
        return profileRepository.getProfile(userId)
    }

    suspend fun updateProfile(profile: StudentProfile): Result<StudentProfile> {
        return profileRepository.updateProfile(profile)
    }

    suspend fun getReputation(userId: UserId): StudentReputation {
        return profileRepository.getReputation(userId)
    }
}

class UpdateStudentProfile(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(
        profile: StudentProfile,
        fullName: String,
        major: String,
        university: String,
        bio: String,
        graduationDate: String = profile.graduationDate,
        avatarUrl: String? = profile.avatarUrl
    ): Result<StudentProfile> {
        val updateResult = profile.updateDetails(
            fullName = fullName,
            major = major,
            university = university,
            bio = bio,
            graduationDate = graduationDate,
            avatarUrl = avatarUrl
        )
        return when (updateResult) {
            is Result.Success -> profileRepository.updateProfile(updateResult.data)
            is Result.Error -> updateResult
        }
    }
}

class SubmitStudentReview(
    private val profileRepository: ProfileRepository,
    private val policy: ProfileValidationPolicy = ProfileValidationPolicy()
) {
    suspend operator fun invoke(targetUserId: UserId, review: StudentReview): Result<Unit> {
        val validation = policy.validateNewReview(review)
        if (validation.isError()) {
            return Result.Error((validation as Result.Error).exception)
        }
        return profileRepository.submitReview(targetUserId, review)
    }
}

class DeleteStudentAccount(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val nowMillis: () -> Long = ::currentTimeMillis
) {
    suspend operator fun invoke(userId: UserId): Result<Unit> {
        val timestamp = nowMillis()
        return when (val result = profileRepository.deleteProfile(userId, timestamp)) {
            is Result.Success -> {
                authRepository.signOut()
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }
}
