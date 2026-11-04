package com.jdrms.bulletin.domain.profile.infrastructure.repository

internal object ProfileErrorMapping {
    private val PROFILE_ERROR_RULES = listOf(
        listOf("could not find the table", "schema cache") to
            "Database table not found. Please verify your Supabase schema setup.",
        listOf("unable to resolve host", "failed to connect", "timeout", "request timeout") to
            "Unable to connect to server. Please check your internet connection.",
        listOf("jwt", "unauthorized", "invalid api key", "no api key") to
            "Unauthorized database request. Please check your Supabase API credentials.",
        listOf("row-level security", "rls") to
            "Database permission denied. Please check your Supabase RLS policies.",
        listOf("invalid input syntax for type uuid", "22p02") to
            "Invalid user identifier format."
    )

    fun map(throwable: Throwable): String {
        val message = throwable.message ?: return "An unexpected profile error occurred."
        val lower = message.lowercase()

        for ((patterns, mappedMessage) in PROFILE_ERROR_RULES) {
            if (patterns.any { lower.contains(it) }) {
                return mappedMessage
            }
        }

        val firstLine = message.lines().firstOrNull { it.isNotBlank() }?.trim() ?: "Profile request failed."
        val isTechnicalDump = firstLine.startsWith("url:", ignoreCase = true) ||
            firstLine.startsWith("headers:", ignoreCase = true) ||
            firstLine.startsWith("http method:", ignoreCase = true)

        return if (isTechnicalDump) {
            "Profile request failed. Please check your connection and try again."
        } else {
            firstLine
        }
    }
}
