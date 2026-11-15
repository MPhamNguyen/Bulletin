package com.jdrms.bulletin.core.designsystem

import androidx.compose.ui.text.TextStyle
import com.jdrms.bulletin.core.designsystem.theme.BulletinFieldStyle

/** Optional money-field decoration and surface style. */
class MoneyInputAppearance(
    val currencySymbolAsAdornment: Boolean = false,
    val placeholder: String? = null,
    val style: BulletinFieldStyle = BulletinFieldStyle.Standard,
    val textStyle: TextStyle? = null
) {
    internal val placeholderText: String
        get() = placeholder ?: if (currencySymbolAsAdornment) "0.00" else "$0.00"
}
