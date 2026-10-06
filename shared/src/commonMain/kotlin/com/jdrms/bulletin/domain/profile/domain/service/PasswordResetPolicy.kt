package com.jdrms.bulletin.domain.profile.domain.service

import com.jdrms.bulletin.core.common.Result

object PasswordResetPolicy {
    // Used only by the in-memory/debug repository until email delivery is configured.
    const val TEST_CONFIRMATION_CODE = "123456"

    fun validateCodeFormat(code: String): Result<Unit> {
        val normalizedCode = code.trim()
        return if (normalizedCode.length in 6..8 && normalizedCode.all(Char::isDigit)) {
            Result.Success(Unit)
        } else {
            Result.Error(IllegalArgumentException("Enter the numeric code from your email."))
        }
    }

    fun validateConfirmationCode(code: String): Result<Unit> {
        return if (code.trim() == TEST_CONFIRMATION_CODE) {
            Result.Success(Unit)
        } else {
            Result.Error(IllegalArgumentException("The confirmation code is incorrect."))
        }
    }
}
