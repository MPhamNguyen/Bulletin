package com.jdrms.bulletin.app

import com.jdrms.bulletin.app.navigation.AppRootScreen

/**
 * Decides where an in-app message banner may appear. Every root screen is listed explicitly, with no `else`
 * branch, so adding a screen forces a conscious allow or deny here. The conversation that is already open is
 * excluded upstream by the alert detector.
 */
internal object InAppBannerScreenPolicy {
    fun allows(screen: AppRootScreen, isPasswordRecovery: Boolean): Boolean {
        if (isPasswordRecovery) return false
        return when (screen) {
            AppRootScreen.SIGN_IN -> false
            AppRootScreen.CREATE_PROFILE -> false
            AppRootScreen.MAIN -> true
        }
    }
}
