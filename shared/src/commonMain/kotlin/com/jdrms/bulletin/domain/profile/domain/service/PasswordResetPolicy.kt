package com.jdrms.bulletin.domain.profile.domain.service

import com.jdrms.bulletin.core.common.Result

object PasswordResetPolicy {
    fun validateCodeFormat(code: String): Result<Unit> {
        val normalizedCode = code.trim()
        return if (normalizedCode.length in 6..8 && normalizedCode.all(Char::isDigit)) {
            Result.Success(Unit)
        } else {
            Result.Error(IllegalArgumentException("Enter the numeric code from your email."))
        }
    }
}
