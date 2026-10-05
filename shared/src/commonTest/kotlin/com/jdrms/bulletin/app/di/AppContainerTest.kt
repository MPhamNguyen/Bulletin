package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.network.SupabaseConfig
import com.jdrms.bulletin.domain.messages.application.MessagingAuthenticationRequiredException
import com.jdrms.bulletin.domain.messages.infrastructure.repository.InMemoryMessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesRepository
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class AppContainerTest {

    @Test
    fun configuredContainerUsesSupabaseMessagesRepository() {
        val container = AppContainer(
            providedSupabaseClient = createSupabaseClient(
                supabaseUrl = "https://example.supabase.co",
                supabaseKey = "test-anon-key"
            ) {
                install(Postgrest)
            }
        )

        assertIs<SupabaseMessagesRepository>(container.messagesRepository)
    }

    @Test
    fun messagingUsesTheSameAuthSessionAndKeepsInMemoryStorage() = runTest {
        val container = AppContainer(isInspectionMode = true)
        assertIs<InMemoryMessagesRepository>(container.messagesRepository)
        assertIs<MessagingAuthenticationRequiredException>(
            assertIs<Result.Error>(container.getConversations()).exception
        )
        val profile = container.authRepository.register(
            StudentEmail("new.student@example.edu"),
            "password123",
            "New Student"
        ).getOrThrow()
        val sender = container.currentMessageSenderProvider.getCurrentSender().getOrThrow()
        assertEquals(profile.id.value, sender.id.value)
        assertEquals(profile.fullName, sender.displayName)
        assertEquals(emptyList(), container.getConversations().getOrThrow())
        container.signOutUser().getOrThrow()
        assertIs<MessagingAuthenticationRequiredException>(
            assertIs<Result.Error>(container.getConversations()).exception
        )
    }

    @Test
    fun testAppContainerRejectsInMemoryFallbackWhenDisabled() {
        val container = AppContainer(
            supabaseConfig = SupabaseConfig(isConnected = false),
            isInspectionMode = false,
            allowInMemoryFallback = false
        )

        assertFailsWith<IllegalStateException> {
            container.profileRepository
        }

        assertFailsWith<IllegalStateException> {
            container.authRepository
        }

        assertFailsWith<IllegalStateException> {
            container.messagesRepository
        }
    }
}
