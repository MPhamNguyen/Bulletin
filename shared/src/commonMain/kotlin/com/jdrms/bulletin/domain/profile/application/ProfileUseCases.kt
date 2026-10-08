package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationCode
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationOutcome
import com.jdrms.bulletin.domain.profile.domain.model.ProfilePhoto
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfilePhotoRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import com.jdrms.bulletin.domain.profile.domain.service.PasswordResetPolicy
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import kotlin.time.Instant

class RequestPasswordReset(
    private val authRepository: AuthRepository,
    private val policy: ProfileValidationPolicy = ProfileValidationPolicy()
) {
    suspend operator fun invoke(email: StudentEmail): Result<Unit> {
        val validation = policy.validateEmail(email.value)
        if (validation.isError()) {
            return Result.Error((validation as Result.Error).exception)
        }
        return authRepository.requestPasswordReset(email)
    }
}

class VerifyPasswordResetCode(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: StudentEmail, code: String): Result<Unit> {
        val validation = PasswordResetPolicy.validateCodeFormat(code)
        if (validation.isError()) {
            return Result.Error((validation as Result.Error).exception)
        }
        return authRepository.verifyPasswordResetCode(email, code)
    }
}

class UpdatePassword(
    private val authRepository: AuthRepository,
    private val policy: ProfileValidationPolicy = ProfileValidationPolicy()
) {
    suspend operator fun invoke(password: String, confirmation: String): Result<Unit> {
        val validation = policy.validatePassword(password)
        if (validation.isError()) {
            return Result.Error((validation as Result.Error).exception)
        }
        if (password != confirmation) {
            return Result.Error(IllegalArgumentException("Passwords do not match."))
        }
        return authRepository.updatePassword(password)
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

class UpdateStudentProfile(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(
        profile: StudentProfile,
        fullName: String,
        major: String,
        university: String,
        bio: String,
        avatarUrl: String? = profile.avatarUrl
    ): Result<StudentProfile> {
        val updateResult = profile.updateDetails(
            fullName = fullName,
            major = major,
            university = university,
            bio = bio,
            avatarUrl = avatarUrl
        )
        return when (updateResult) {
            is Result.Success -> profileRepository.updateProfile(updateResult.data)
            is Result.Error -> updateResult
        }
    }
}

class UploadProfilePhoto(
    private val profilePhotoRepository: ProfilePhotoRepository,
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(
        profile: StudentProfile,
        bytes: ByteArray,
        mediaType: String
    ): Result<StudentProfile> {
        val photo = when (val result = ProfilePhoto.create(bytes, mediaType)) {
            is Result.Success -> result.data
            is Result.Error -> return result
        }
        return when (val uploadResult = profilePhotoRepository.upload(profile.id, photo)) {
            is Result.Success -> profileRepository.updateProfile(profile.changeProfilePhoto(uploadResult.data))
            is Result.Error -> uploadResult
        }
    }
}

class SoftDeleteProfile(
    private val profileRepository: ProfileRepository,
    private val signOutUser: SignOutUser,
    private val nowTimestamp: () -> String = {
        Instant.fromEpochMilliseconds(currentTimeMillis()).toString()
    }
) {
    suspend operator fun invoke(profile: StudentProfile): Result<StudentProfile> {
        val timestamp = nowTimestamp()
        val deleteResult = profile.softDelete(timestamp)
        if (deleteResult is Result.Error) return deleteResult
        val deletedProfile = (deleteResult as Result.Success).data

        return when (val repoResult = profileRepository.softDelete(deletedProfile.id, timestamp)) {
            is Result.Success -> {
                when (val signOutResult = signOutUser()) {
                    is Result.Success -> Result.Success(deletedProfile)
                    is Result.Error -> signOutResult
                }
            }
            is Result.Error -> repoResult
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
