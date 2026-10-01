package com.jdrms.bulletin.app.theme

import com.jdrms.bulletin.domain.profile.domain.model.UserId

interface ThemePreferenceStore {
    fun preferenceFor(userId: UserId): ThemePreference

    fun setPreference(userId: UserId, preference: ThemePreference)
}

class InMemoryThemePreferenceStore(
    initialPreferences: Map<UserId, ThemePreference> = emptyMap()
) : ThemePreferenceStore {
    private val preferences = initialPreferences.toMutableMap()

    override fun preferenceFor(userId: UserId): ThemePreference {
        return preferences[userId] ?: ThemePreference.SYSTEM
    }

    override fun setPreference(userId: UserId, preference: ThemePreference) {
        preferences[userId] = preference
    }
}
