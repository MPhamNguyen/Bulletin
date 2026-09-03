package com.jdrms.bulletin.app

import com.jdrms.bulletin.app.navigation.AppRootScreen
import com.jdrms.bulletin.domain.profile.presentation.AuthSessionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AppNavigationTest {

    @Test
    fun testAuthenticatedSessionOpensMainApp() {
        assertEquals(
            AppRootScreen.MAIN,
            resolveRootScreen(AppRootScreen.SIGN_IN, AuthSessionState.AUTHENTICATED)
        )
    }

    @Test
    fun testUnauthenticatedSessionLeavesSignUpFlowOpen() {
        assertEquals(
            AppRootScreen.CREATE_PROFILE,
            resolveRootScreen(AppRootScreen.CREATE_PROFILE, AuthSessionState.UNAUTHENTICATED)
        )
    }

    @Test
    fun testSignedOutSessionReturnsMainAppToSignIn() {
        assertEquals(
            AppRootScreen.SIGN_IN,
            resolveRootScreen(AppRootScreen.MAIN, AuthSessionState.UNAUTHENTICATED)
        )
    }

    @Test
    fun testProvidedDependencyDoesNotCreateFallback() {
        val provided = Any()
        var fallbackCreationCount = 0

        val resolved = resolveProvidedOrCreate(provided) {
            fallbackCreationCount += 1
            Any()
        }

        assertSame(provided, resolved)
        assertEquals(0, fallbackCreationCount)
    }
}
