package com.jdrms.bulletin.domain.profile.domain.model

import com.jdrms.bulletin.core.common.Result

/** Registration is pending until the authentication provider confirms ownership of this email. */
data class PendingRegistration(val email: StudentEmail)

class EmailVerificationCode private constructor(val value: String) {
    override fun toString(): String = "EmailVerificationCode([redacted])"

    companion object {
        private val CODE_PATTERN = Regex("[0-9]{6}")

        fun parse(raw: String): Result<EmailVerificationCode> {
            val normalized = raw.trim()
            return if (CODE_PATTERN.matches(normalized)) {
                Result.Success(EmailVerificationCode(normalized))
            } else {
                Result.Error(IllegalArgumentException("Enter the 6-digit code from your email."))
            }
        }
    }
}
