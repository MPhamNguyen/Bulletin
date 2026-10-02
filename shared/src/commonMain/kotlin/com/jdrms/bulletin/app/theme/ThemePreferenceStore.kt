package com.jdrms.bulletin.app.theme

import com.jdrms.bulletin.domain.profile.domain.model.UserId

interface ThemePreferenceStore {
    suspend fun lastAppliedPreference(): ThemePreference

    suspend fun setLastAppliedPreference(preference: ThemePreference)

    suspend fun preferenceFor(userId: UserId): ThemePreference

    suspend fun setPreference(userId: UserId, preference: ThemePreference)
}

class InMemoryThemePreferenceStore(
    initialPreferences: Map<UserId, ThemePreference> = emptyMap(),
    initialLastAppliedPreference: ThemePreference = ThemePreference.SYSTEM
) : ThemePreferenceStore {
    private val preferences = initialPreferences.toMutableMap()
    private var lastAppliedPreference = initialLastAppliedPreference

    override suspend fun lastAppliedPreference(): ThemePreference = lastAppliedPreference

    override suspend fun setLastAppliedPreference(preference: ThemePreference) {
        lastAppliedPreference = preference
    }

    override suspend fun preferenceFor(userId: UserId): ThemePreference {
        return preferences[userId] ?: ThemePreference.SYSTEM
    }

    override suspend fun setPreference(userId: UserId, preference: ThemePreference) {
        preferences[userId] = preference
        setLastAppliedPreference(preference)
    }
}
