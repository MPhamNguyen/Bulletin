package com.jdrms.bulletin.app.theme

import com.jdrms.bulletin.core.designsystem.theme.BulletinThemeColor
import com.jdrms.bulletin.domain.profile.domain.model.UserId

interface ThemeBaseColorPreferenceStore {
    suspend fun lastAppliedBaseColor(): BulletinThemeColor

    suspend fun setLastAppliedBaseColor(baseColor: BulletinThemeColor)

    suspend fun baseColorFor(userId: UserId): BulletinThemeColor

    suspend fun setBaseColor(userId: UserId, baseColor: BulletinThemeColor)
}
