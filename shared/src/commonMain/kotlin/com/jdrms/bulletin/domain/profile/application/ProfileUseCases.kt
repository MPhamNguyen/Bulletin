package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.core.common.Result
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
        return authRepository.login(email, password)
    }

    suspend fun register(
        email: StudentEmail,
        password: String,
        fullName: String,
        university: String = "CSU Long Beach"
    ): Result<StudentProfile> {
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
    suspend operator fun invoke(email: StudentEmail, code: String): Result<Boolean> {
        return authRepository.verifyEmail(email, code)
    }
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
        bio: String
    ): Result<StudentProfile> {
        return when (val updatedProfile = profile.updateDetails(fullName, major, university, bio)) {
            is Result.Success -> profileRepository.updateProfile(updatedProfile.data)
            is Result.Error -> updatedProfile
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
