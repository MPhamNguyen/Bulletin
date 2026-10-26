package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.network.SupabaseConfig
import com.jdrms.bulletin.domain.messages.application.MessagingAuthenticationRequiredException
import com.jdrms.bulletin.domain.messages.infrastructure.repository.InMemoryMessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesObserver
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesRepository
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

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
        assertIs<SupabaseMessagesObserver>(container.messagesObserver)
    }

    @Test
    fun messagingUsesTheSameAuthSessionAndKeepsInMemoryStorage() = runTest {
        val container = AppContainer(isInspectionMode = true)
        assertIs<InMemoryMessagesRepository>(container.messagesRepository)
        assertSame<Any>(container.messagesRepository, container.messagesObserver)
        assertSame<Any>(container.messagesRepository, container.inboxObserver)
        assertIs<MessagingAuthenticationRequiredException>(
            assertIs<Result.Error>(container.getConversations()).exception
        )
        val profile = container.authRepository.login(
            StudentEmail("dominic.alfonso@student.csulb.edu"),
            "password123"
        ).getOrThrow()
        val sender = container.currentMessageSenderProvider.getCurrentSender().getOrThrow()
        assertEquals(profile.id.value, sender.id.value)
        assertEquals(profile.fullName, sender.displayName)
        assertEquals(emptyList(), container.getConversations().getOrThrow().conversations)
        container.signOutUser().getOrThrow()
        assertIs<MessagingAuthenticationRequiredException>(
            assertIs<Result.Error>(container.getConversations()).exception
        )
    }

    @Test
    fun testAppContainerRejectsInMemoryFallbackWhenDisabled() {
        val container = AppContainer(
            supabaseConfig = SupabaseConfig(isConnected = false),
            isInspectionMode = false
        )

        backendBindings(container).forEach { binding ->
            val failure = assertFailsWith<IllegalStateException> { binding() }
            assertEquals(
                "Supabase client is not configured and in-memory fallback is disabled.",
                failure.message
            )
        }
    }

    @Test
    fun explicitFallbackAndInspectionAllowAllBackendBindings() {
        listOf(
            AppContainer(allowInMemoryFallback = true),
            AppContainer(isInspectionMode = true)
        ).forEach { container ->
            backendBindings(container).forEach { it() }
            assertSame<Any>(container.inboxObserver, container.messagesRepository)
            assertSame<Any>(container.messagesObserver, container.messagesRepository)
            assertSame(container.signInUser, container.signInUser)
        }
    }

    private fun backendBindings(container: AppContainer): List<() -> Any> = listOf(
        { container.profileRepository },
        { container.authRepository },
        { container.messagesRepository },
        { container.messagesObserver },
        { container.inboxObserver },
        { container.profilePhotoRepository },
        { container.listingsRepository },
        { container.listingImagesRepository },
        { container.marketplaceRepository },
        { container.marketplaceListingSource },
        { container.reportMarketplaceListing },
        { container.reportMarketplaceUser },
        { container.blockMarketplaceUser }
    )
}
