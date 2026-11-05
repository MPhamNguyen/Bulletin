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
    initialLastAppliedThemeColor: BulletinThemeColor = BulletinThemeColor.CLASSIC,
    initialBaseColors: Map<UserId, BulletinThemeColor> = emptyMap(),
    initialLastAppliedBaseColor: BulletinThemeColor = BulletinThemeColor.BASE
) : ThemePreferenceStores {
    private val preferences = initialPreferences.toMutableMap()
    private var lastAppliedPreference = initialLastAppliedPreference
    private val themeColors = initialThemeColors.toMutableMap()
    private var lastAppliedThemeColor = initialLastAppliedThemeColor
    private val baseColors = initialBaseColors.toMutableMap()
    private var lastAppliedBaseColor = initialLastAppliedBaseColor

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

    override suspend fun lastAppliedBaseColor(): BulletinThemeColor = lastAppliedBaseColor

    override suspend fun setLastAppliedBaseColor(baseColor: BulletinThemeColor) {
        lastAppliedBaseColor = baseColor
    }

    override suspend fun baseColorFor(userId: UserId): BulletinThemeColor =
        baseColors[userId] ?: BulletinThemeColor.BASE

    override suspend fun setBaseColor(userId: UserId, baseColor: BulletinThemeColor) {
        baseColors[userId] = baseColor
        setLastAppliedBaseColor(baseColor)
    }

    override suspend fun setThemeColors(
        userId: UserId,
        baseColor: BulletinThemeColor,
        themeColor: BulletinThemeColor
    ) {
        baseColors[userId] = baseColor
        themeColors[userId] = themeColor
        lastAppliedBaseColor = baseColor
        lastAppliedThemeColor = themeColor
    }

    override suspend fun setLastAppliedThemeColors(baseColor: BulletinThemeColor, themeColor: BulletinThemeColor) {
        lastAppliedBaseColor = baseColor
        lastAppliedThemeColor = themeColor
    }
}
