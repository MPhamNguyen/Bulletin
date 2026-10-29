package com.jdrms.bulletin.core.designsystem.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/** Shared spacing scale, screen gutter, and minimum control height. */
object BulletinSpacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val gutter = l
    val control = 48.dp
}

// Artwork stays dark in either app theme so overlaid listing details remain readable.
object BulletinFeedCardDefaults {
    val backgroundTop = Color(0xFF27405A)
    val backgroundMiddle = PaletteMidBlue
    val backgroundBottom = PaletteSlateBlue
    val upperCircleTop = Color(0xFF5D7B99)
    val upperCircleBottom = Color(0xFF86A0BD)
    val leftCircleTop = Color(0xFF9AA9BF)
    val leftCircleBottom = Color(0xFFB5BFD0)
    val lowerCircleTop = Color(0xFF4B6A88)
    val lowerCircleBottom = Color(0xFF33506B)
    val scrim = Color(0xFF070B13)
    val content = Color(0xFFF0F4F8)
    val secondaryContent = Color(0xFFD3E0F0)

    // Controls stay over dark artwork in both app themes.
    val actionPassTint = Color(0xFFFF8A80)
    val actionSaveTint = Color(0xFF6FD1AC)
    val actionGlass = Color.White.copy(alpha = 0.14f)
    val actionGlassOutline = Color.White.copy(alpha = 0.28f)
    val actionGlassHighlight = Color.White.copy(alpha = 0.25f)
    val actionMessageOutline = Color.White.copy(alpha = 0.35f)
    val actionMessageStart = Color(0xFF6382AB)
    val actionMessageEnd = Color(0xFF3A5272)
    val actionFocusRing = Color.White
    val actionShape = CircleShape
}

object BulletinConversationDefaults {
    private val isDark: Boolean
        @Composable get() = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val unreadGradientStart: Color
        @Composable get() = if (isDark) Color(0xFF193A67) else Color(0xFFDCEBFF)
    val unreadGradientEnd: Color
        @Composable get() = if (isDark) Color(0xFF22495D) else Color(0xFFDDF3F0)
    val unreadBorder: Color
        @Composable get() = if (isDark) Color(0xFF77A7F7) else Color(0xFF3E5C76)
}

object BulletinTextFieldDefaults {
    @Composable
    fun colors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    )
}

object BulletinInactiveButtonDefaults {
    @Composable
    fun colors(): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
    )
}

object BulletinButtonDefaults {
    // Primary CTAs (Sign in, Verify Email, Post Listing, Message Seller,
    // Create Account, Update) use `primary`, not tertiary.
    @Composable
    fun buttonColors(): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )

    @Composable
    fun inactiveButtonColors(): ButtonColors = BulletinInactiveButtonDefaults.colors()

    @Composable
    fun outlinedButtonColors(): ButtonColors = ButtonDefaults.outlinedButtonColors(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary
    )

    @Composable
    fun outlinedButtonBorder(): BorderStroke = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )

    @Composable
    fun destructiveButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError
    )

    @Composable
    fun destructiveOutlinedButtonColors(): ButtonColors = ButtonDefaults.outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
        contentColor = MaterialTheme.colorScheme.error
    )

    @Composable
    fun destructiveOutlinedButtonBorder(): BorderStroke = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
    )

    @Composable
    fun neutralButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    )

    @Composable
    fun textButtonColors(): ButtonColors = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.primary
    )
}
