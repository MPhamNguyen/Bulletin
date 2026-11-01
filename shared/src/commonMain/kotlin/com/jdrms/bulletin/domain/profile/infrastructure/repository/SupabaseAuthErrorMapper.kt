package com.jdrms.bulletin.domain.profile.infrastructure.repository

internal object SupabaseAuthErrorMapper {
    private val COMMON_ERROR_RULES = listOf(
        listOf("otp_expired", "token has expired", "token is invalid") to
            "This code is invalid or has expired. Request a new code and try again.",
        listOf("over_request_rate_limit", "over_email_send_rate_limit", "email rate limit exceeded") to
            "Too many signup attempts. Please wait a few minutes before trying again.",
        listOf("user_already_exists", "user already registered") to
            "An account with this email address already exists. Please log in instead.",
        listOf("invalid_credentials", "invalid login credentials") to
            "Invalid email or password. Please try again.",
        listOf("email_address_invalid", "invalid email") to
            "Invalid email address. Please use a valid university or personal email domain.",
        listOf("bulletin requires a valid .edu university email") to
            "Bulletin requires a valid .edu university email.",
        listOf("signup_disabled", "signups not allowed") to
            "Account registration is currently disabled.",
        listOf("email_not_confirmed") to
            "Please verify your email address before logging in.",
        listOf("invalid input syntax for type uuid", "invalid input syntax", "22p02") to
            "The requested user account was not found.",
        listOf("jwt expired", "invalid jwt", "pgrst301", "auth_token_expired") to
            "Your session has expired. Please log in again.",
        listOf("row-level security", "permission denied", "42501") to
            "You do not have permission to perform this action.",
        listOf("unable to resolve host", "failed to connect", "timeout", "connectexception", "sockettimeout") to
            "Unable to connect to server. Please check your internet connection."
    )

    private val TECHNICAL_PATTERNS = listOf(
        "http",
        "header",
        "url:",
        "rest/v1",
        "supabase.co",
        "select=",
        "apikey",
        "syntax"
    )

    fun mapErrorMessage(
        throwable: Throwable,
        defaultMessage: String = "An unexpected error occurred."
    ): String {
        val message = throwable.message ?: return defaultMessage
        val lower = message.lowercase()

        for ((patterns, mappedMessage) in COMMON_ERROR_RULES) {
            if (patterns.any { lower.contains(it) }) {
                return mappedMessage
            }
        }

        val firstLine = message.lines().firstOrNull()?.trim() ?: defaultMessage
        val isTechnicalDump = TECHNICAL_PATTERNS.any { lower.contains(it) }

        return if (isTechnicalDump) {
            defaultMessage
        } else {
            firstLine
        }
    }

    fun mapAuthErrorMessage(throwable: Throwable): String {
        return mapErrorMessage(
            throwable = throwable,
            defaultMessage = "Authentication failed. Please check your details and try again."
        )
    }
}
