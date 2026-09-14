package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.core.network.SupabaseConfig
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AppContainerTest {

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
    }
}
