package com.jdrms.bulletin.app.theme

import com.jdrms.bulletin.domain.profile.domain.model.UserId

interface ThemePreferenceStore {
    fun lastAppliedPreference(): ThemePreference

    fun setLastAppliedPreference(preference: ThemePreference)

    fun preferenceFor(userId: UserId): ThemePreference

    fun setPreference(userId: UserId, preference: ThemePreference)
}

class InMemoryThemePreferenceStore(
    initialPreferences: Map<UserId, ThemePreference> = emptyMap(),
    initialLastAppliedPreference: ThemePreference = ThemePreference.SYSTEM
) : ThemePreferenceStore {
    private val preferences = initialPreferences.toMutableMap()
    private var lastAppliedPreference = initialLastAppliedPreference

    override fun lastAppliedPreference(): ThemePreference = lastAppliedPreference

    override fun setLastAppliedPreference(preference: ThemePreference) {
        lastAppliedPreference = preference
    }

    override fun preferenceFor(userId: UserId): ThemePreference {
        return preferences[userId] ?: ThemePreference.SYSTEM
    }

    override fun setPreference(userId: UserId, preference: ThemePreference) {
        preferences[userId] = preference
        setLastAppliedPreference(preference)
    }
}
