package com.jdrms.bulletin.domain.profile.domain.service

import com.jdrms.bulletin.core.common.Result

object PasswordResetPolicy {
    // Temporary deterministic value until the email provider is configured.
    const val TEST_CONFIRMATION_CODE = "123456"

    fun validateConfirmationCode(code: String): Result<Unit> {
        return if (code.trim() == TEST_CONFIRMATION_CODE) {
            Result.Success(Unit)
        } else {
            Result.Error(IllegalArgumentException("The confirmation code is incorrect."))
        }
    }
}
