package com.jdrms.bulletin.app.theme

import android.content.Context
import com.jdrms.bulletin.domain.profile.domain.model.UserId

class AndroidThemePreferenceStore(
    context: Context
) : ThemePreferenceStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun lastAppliedPreference(): ThemePreference {
        return preferenceFromStoredValue(preferences.getString(LAST_APPLIED_KEY, null))
    }

    override fun setLastAppliedPreference(preference: ThemePreference) {
        preferences.edit().putString(LAST_APPLIED_KEY, preference.name).apply()
    }

    override fun preferenceFor(userId: UserId): ThemePreference {
        return preferenceFromStoredValue(preferences.getString(keyFor(userId), null))
    }

    override fun setPreference(userId: UserId, preference: ThemePreference) {
        preferences.edit()
            .putString(keyFor(userId), preference.name)
            .apply()
        setLastAppliedPreference(preference)
    }

    private fun preferenceFromStoredValue(value: String?): ThemePreference {
        return ThemePreference.entries.firstOrNull { it.name == value } ?: ThemePreference.SYSTEM
    }

    private fun keyFor(userId: UserId): String = "$KEY_PREFIX${userId.value}"

    private companion object {
        const val PREFERENCES_NAME = "bulletin_theme_preferences"
        const val KEY_PREFIX = "theme_"
        const val LAST_APPLIED_KEY = "last_applied_theme"
    }
}
