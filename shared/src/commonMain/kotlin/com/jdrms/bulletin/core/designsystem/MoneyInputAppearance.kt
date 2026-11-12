package com.jdrms.bulletin.core.designsystem

import com.jdrms.bulletin.core.designsystem.theme.BulletinFieldStyle

/** Optional money-field decoration and surface style. */
class MoneyInputAppearance(
    val currencySymbolAsAdornment: Boolean = false,
    val placeholder: String? = null,
    val style: BulletinFieldStyle = BulletinFieldStyle.Standard
)
