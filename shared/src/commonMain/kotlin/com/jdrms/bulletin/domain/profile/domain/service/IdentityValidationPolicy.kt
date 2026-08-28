package com.jdrms.bulletin.domain.profile.domain.service

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.Email
import com.jdrms.bulletin.domain.profile.domain.model.Profile

class IdentityValidationPolicy {
    fun validateUniversityRegistration(emailRaw: String): Result<Email> {
        return try {
            val email = Email(emailRaw)
            if (!email.isUniversityEmail) {
                Result.Error(IllegalArgumentException("Must register with a valid university (.edu) email address."))
            } else {
                Result.Success(email)
            }
        } catch (e: IllegalArgumentException) {
            Result.Error(e)
        }
    }

    fun validateProfileUpdate(profile: Profile): Result<Unit> {
        if (profile.fullName.isBlank()) {
            return Result.Error(IllegalArgumentException("Full name cannot be blank."))
        }
        if (profile.university.isBlank()) {
            return Result.Error(IllegalArgumentException("University affiliation cannot be blank."))
        }
        return Result.Success(Unit)
    }
}
