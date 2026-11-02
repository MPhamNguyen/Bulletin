package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseProfileRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class SupabaseErrorMessageMappingTest {

    @Test
    fun testSupabaseAuthRepositoryErrorMapping() {
        val userExistsError = Exception(
            "user_already_exists (User already registered: user_already_exists)\n" +
                "URL: https://example.supabase.co/auth/v1/signup\n" +
                "Headers: [Authorization=[Bearer token123], apikey=[key123]]"
        )
        assertEquals(
            "An account with this email address already exists. Please log in instead.",
            SupabaseAuthRepository.mapAuthErrorMessage(userExistsError)
        )

        val rateLimitError = Exception("over_email_send_rate_limit (email rate limit exceeded)")
        assertEquals(
            "Too many signup attempts. Please wait a few minutes before trying again.",
            SupabaseAuthRepository.mapAuthErrorMessage(rateLimitError)
        )

        val invalidCredentialsError = Exception("invalid_credentials (Invalid login credentials)")
        assertEquals(
            "Invalid email or password. Please try again.",
            SupabaseAuthRepository.mapAuthErrorMessage(invalidCredentialsError)
        )

        val invalidEmailError = Exception("email_address_invalid (Email address is invalid)")
        assertEquals(
            "Invalid email address. Please use a valid university or personal email domain.",
            SupabaseAuthRepository.mapAuthErrorMessage(invalidEmailError)
        )

        val connectionError = Exception("Failed to connect to host (timeout)")
        assertEquals(
            "Unable to connect to server. Please check your internet connection.",
            SupabaseAuthRepository.mapAuthErrorMessage(connectionError)
        )
    }

    @Test
    fun testSupabaseProfileRepositorySchemaAndConnectionErrorMapping() {
        val schemaCacheError = Exception(
            "Could not find the table `public.profiles` in the schema cache.\n\n" +
                "URL:\n" +
                "https://qullzprtorshyqkxuvac.supabase.co/rest/v1/profiles?id=eq.current_student&select=%2A\n\n" +
                "Headers:\n\n" +
                "* Authorization: Bearer [redacted]\n" +
                "* Content-Type: application/json\n" +
                "* Prefer:\n" +
                "* Accept-Profile: public\n" +
                "* apikey: [redacted]\n" +
                "* X-Client-Info: supabase-kt/3.1.4\n" +
                "* Accept: application/json\n" +
                "* Accept-Charset: UTF-8\n\n" +
                "HTTP method: GET"
        )
        assertEquals(
            "Database table not found. Please verify your Supabase schema setup.",
            SupabaseProfileRepository.mapProfileErrorMessage(schemaCacheError)
        )

        val timeoutError = Exception(
            "Request timeout has expired " +
                "[url=https://qullzprtorshyqkxuvac.supabase.co/rest/v1/profiles?id=eq.current_student&select=%2A, " +
                "request_timeout=10000 ms]"
        )
        assertEquals(
            "Unable to connect to server. Please check your internet connection.",
            SupabaseProfileRepository.mapProfileErrorMessage(timeoutError)
        )
    }

    @Test
    fun testSupabaseProfileRepositoryAuthorizationErrorMapping() {
        val unauthorizedError = Exception("invalid api key provided in apikey header")
        assertEquals(
            "Unauthorized database request. Please check your Supabase API credentials.",
            SupabaseProfileRepository.mapProfileErrorMessage(unauthorizedError)
        )

        val rlsError = Exception("new row violates row-level security policy for table profiles")
        assertEquals(
            "Database permission denied. Please check your Supabase RLS policies.",
            SupabaseProfileRepository.mapProfileErrorMessage(rlsError)
        )
    }

    @Test
    fun testSupabaseProfileRepositoryValidationAndCustomErrorMapping() {
        val customFirstLineError = Exception("User profile is temporarily locked by administrator.\nURL: https://...\n")
        assertEquals(
            "User profile is temporarily locked by administrator.",
            SupabaseProfileRepository.mapProfileErrorMessage(customFirstLineError)
        )

        val uuidSyntaxError = Exception("invalid input syntax for type uuid: \"current_student\"")
        assertEquals(
            "Invalid user identifier format.",
            SupabaseProfileRepository.mapProfileErrorMessage(uuidSyntaxError)
        )
    }
}
