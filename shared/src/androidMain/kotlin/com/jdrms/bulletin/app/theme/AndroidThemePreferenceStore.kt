package com.jdrms.bulletin.app.theme

import android.content.Context
import com.jdrms.bulletin.domain.profile.domain.model.UserId

class AndroidThemePreferenceStore(
    context: Context
) : ThemePreferenceStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun preferenceFor(userId: UserId): ThemePreference {
        val storedValue = preferences.getString(keyFor(userId), null) ?: return ThemePreference.SYSTEM
        return runCatching { ThemePreference.valueOf(storedValue) }
            .getOrDefault(ThemePreference.SYSTEM)
    }

    override fun setPreference(userId: UserId, preference: ThemePreference) {
        preferences.edit().putString(keyFor(userId), preference.name).apply()
    }

    private fun keyFor(userId: UserId): String = "$KEY_PREFIX${userId.value}"

    private companion object {
        const val PREFERENCES_NAME = "bulletin_theme_preferences"
        const val KEY_PREFIX = "theme_"
    }
}
