package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.ui.graphics.vector.ImageVector
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition

internal fun ListingCategory.icon(): ImageVector = when (this) {
    ListingCategory.TEXTBOOKS -> Icons.AutoMirrored.Outlined.MenuBook
    ListingCategory.ELECTRONICS -> Icons.Outlined.Keyboard
    ListingCategory.FURNITURE -> Icons.Outlined.Chair
    ListingCategory.CLOTHING -> Icons.Outlined.Checkroom
    ListingCategory.HOUSING -> Icons.Outlined.Home
    ListingCategory.OTHER -> Icons.Outlined.Category
}

internal fun ListingCategory.displayLabel(): String = name.toSentenceCase()

internal fun ListingCondition.displayLabel(): String = name.toSentenceCase()

private fun String.toSentenceCase(): String =
    lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
