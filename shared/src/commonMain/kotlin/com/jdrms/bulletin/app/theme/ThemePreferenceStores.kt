package com.jdrms.bulletin.app.theme

import com.jdrms.bulletin.core.designsystem.theme.BulletinThemeColor
import com.jdrms.bulletin.domain.profile.domain.model.UserId

interface ThemePreferenceStores : ThemePreferenceStore, ThemeBaseColorPreferenceStore {
    suspend fun setThemeColors(userId: UserId, baseColor: BulletinThemeColor, themeColor: BulletinThemeColor)

    suspend fun setLastAppliedThemeColors(baseColor: BulletinThemeColor, themeColor: BulletinThemeColor)
}
