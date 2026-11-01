package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

/** Tracks the request, code verification, and password update steps of one recovery flow. */
internal class SupabasePasswordRecovery(
    private val supabase: SupabaseClient
) {
    private var requested = false
    private var codeVerified = false

    suspend fun request(email: StudentEmail): Result<Unit> {
        return runCatching {
            supabase.auth.resetPasswordForEmail(email = email.value)
            requested = true
            codeVerified = false
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(Exception(SupabaseAuthErrorMapper.mapAuthErrorMessage(it), it)) }
        )
    }

    suspend fun verifyCode(email: StudentEmail, code: String): Result<Unit> {
        return if (!requested) {
            Result.Error(IllegalStateException("Request a new password reset before entering a code."))
        } else {
            runCatching {
                supabase.auth.verifyEmailOtp(
                    type = OtpType.Email.RECOVERY,
                    email = email.value,
                    token = code.trim()
                )
                codeVerified = true
            }.fold(
                onSuccess = { Result.Success(Unit) },
                onFailure = { Result.Error(Exception(SupabaseAuthErrorMapper.mapAuthErrorMessage(it), it)) }
            )
        }
    }

    suspend fun updatePassword(password: String): Result<Unit> {
        if (!codeVerified) {
            return Result.Error(IllegalStateException("Verify the confirmation code before choosing a new password."))
        }
        return runCatching {
            supabase.auth.updateUser {
                this.password = password
            }
            requested = false
            codeVerified = false
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(Exception(SupabaseAuthErrorMapper.mapAuthErrorMessage(it), it)) }
        )
    }
}
