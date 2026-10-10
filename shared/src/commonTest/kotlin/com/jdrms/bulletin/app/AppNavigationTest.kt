package com.jdrms.bulletin.app

import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.AppRootScreen
import com.jdrms.bulletin.app.navigation.MainNavigationState
import com.jdrms.bulletin.app.navigation.ProfileDestination
import com.jdrms.bulletin.app.navigation.backFromProfile
import com.jdrms.bulletin.app.navigation.backFromProfileDestination
import com.jdrms.bulletin.app.navigation.explicitProfileBack
import com.jdrms.bulletin.app.navigation.systemProfileBack
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
    fun authenticatedRegistrationKeepsTheSuccessScreenOpen() {
        assertEquals(
            AppRootScreen.CREATE_PROFILE,
            resolveRootScreen(AppRootScreen.CREATE_PROFILE, AuthSessionState.AUTHENTICATED)
        )
    }

    @Test
    fun profileSubscreenBackRoutesAreOwnedByNavigation() {
        assertEquals(ProfileDestination.PROFILE, backFromProfile(ProfileDestination.SETTINGS))
        assertEquals(ProfileDestination.PROFILE, backFromProfile(ProfileDestination.BOOKMARKED_LISTINGS))
        assertEquals(ProfileDestination.SETTINGS, backFromProfile(ProfileDestination.NOTIFICATIONS))
        assertEquals(ProfileDestination.SETTINGS, backFromProfile(ProfileDestination.PUBLIC_PROFILE))
        assertEquals(
            ProfileDestination.SETTINGS,
            backFromProfileDestination(ProfileDestination.EDIT_ACCOUNT, ProfileDestination.SETTINGS)
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
    fun testCheckingSessionPreservesTheCurrentRootUntilAuthenticationRestorationCompletes() {
        assertEquals(
            AppRootScreen.CREATE_PROFILE,
            resolveRootScreen(AppRootScreen.CREATE_PROFILE, AuthSessionState.CHECKING)
        )
        assertEquals(
            AppRootScreen.MAIN,
            resolveRootScreen(AppRootScreen.MAIN, AuthSessionState.CHECKING)
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

    @Test
    fun backFromMyListingsReturnsToProfile() {
        assertEquals(ProfileDestination.PROFILE, backFromProfile(ProfileDestination.MY_LISTINGS))
    }

    @Test
    fun backFromEditListingReturnsToMyListings() {
        assertEquals(ProfileDestination.MY_LISTINGS, backFromProfile(ProfileDestination.EDIT_LISTING))
    }

    @Test
    fun selectingProfileFromMyListingsReturnsToMainProfile() {
        val state = MainNavigationState(
            currentDestination = AppDestination.PROFILE,
            profileDestination = ProfileDestination.MY_LISTINGS
        )

        val result = state.selectBottomNavigationDestination(AppDestination.PROFILE)

        assertEquals(AppDestination.PROFILE, result.currentDestination)
        assertEquals(ProfileDestination.PROFILE, result.profileDestination)
    }

    @Test
    fun explicitBackCancelsEditRouteWithoutDiscardingParentRoute() {
        assertEquals(ProfileDestination.MY_LISTINGS, explicitProfileBack(ProfileDestination.EDIT_LISTING))
    }

    @Test
    fun systemBackCancelsEditRouteWithoutDiscardingParentRoute() {
        assertEquals(ProfileDestination.MY_LISTINGS, systemProfileBack(ProfileDestination.EDIT_LISTING))
    }
}
