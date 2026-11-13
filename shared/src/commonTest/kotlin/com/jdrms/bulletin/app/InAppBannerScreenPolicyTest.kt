package com.jdrms.bulletin.app

import com.jdrms.bulletin.app.navigation.AppRootScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InAppBannerScreenPolicyTest {
    @Test
    fun `authenticated main screens show banners`() {
        assertTrue(InAppBannerScreenPolicy.allows(AppRootScreen.MAIN, isPasswordRecovery = false))
    }

    @Test
    fun `sign-in and profile creation skip banners`() {
        assertFalse(InAppBannerScreenPolicy.allows(AppRootScreen.SIGN_IN, isPasswordRecovery = false))
        assertFalse(InAppBannerScreenPolicy.allows(AppRootScreen.CREATE_PROFILE, isPasswordRecovery = false))
    }

    @Test
    fun `password recovery skips banners on every screen`() {
        AppRootScreen.entries.forEach {
            assertFalse(InAppBannerScreenPolicy.allows(it, isPasswordRecovery = true), it.name)
        }
    }

    @Test
    fun `every root screen has an explicit decision`() {
        val allowed = AppRootScreen.entries.filter { InAppBannerScreenPolicy.allows(it, isPasswordRecovery = false) }
        assertEquals(listOf(AppRootScreen.MAIN), allowed)
    }
}
