package com.jdrms.bulletin.app.theme

import com.jdrms.bulletin.core.designsystem.theme.BulletinThemeColor
import com.jdrms.bulletin.domain.profile.domain.model.UserId

interface ThemePreferenceStore {
    suspend fun lastAppliedPreference(): ThemePreference

    suspend fun setLastAppliedPreference(preference: ThemePreference)

    suspend fun preferenceFor(userId: UserId): ThemePreference

    suspend fun setPreference(userId: UserId, preference: ThemePreference)

    suspend fun lastAppliedThemeColor(): BulletinThemeColor

    suspend fun setLastAppliedThemeColor(themeColor: BulletinThemeColor)

    suspend fun themeColorFor(userId: UserId): BulletinThemeColor

    suspend fun setThemeColor(userId: UserId, themeColor: BulletinThemeColor)
}

class InMemoryThemePreferenceStore(
    initialPreferences: Map<UserId, ThemePreference> = emptyMap(),
    initialLastAppliedPreference: ThemePreference = ThemePreference.SYSTEM,
    initialThemeColors: Map<UserId, BulletinThemeColor> = emptyMap(),
    initialLastAppliedThemeColor: BulletinThemeColor = BulletinThemeColor.CLASSIC
) : ThemePreferenceStore {
    private val preferences = initialPreferences.toMutableMap()
    private var lastAppliedPreference = initialLastAppliedPreference
    private val themeColors = initialThemeColors.toMutableMap()
    private var lastAppliedThemeColor = initialLastAppliedThemeColor

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

    override suspend fun lastAppliedThemeColor(): BulletinThemeColor = lastAppliedThemeColor

    override suspend fun setLastAppliedThemeColor(themeColor: BulletinThemeColor) {
        lastAppliedThemeColor = themeColor
    }

    override suspend fun themeColorFor(userId: UserId): BulletinThemeColor =
        themeColors[userId] ?: BulletinThemeColor.CLASSIC

    override suspend fun setThemeColor(userId: UserId, themeColor: BulletinThemeColor) {
        themeColors[userId] = themeColor
        setLastAppliedThemeColor(themeColor)
    }
}
