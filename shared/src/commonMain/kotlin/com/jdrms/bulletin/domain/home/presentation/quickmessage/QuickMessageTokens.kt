package com.jdrms.bulletin.domain.home.presentation.quickmessage

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal val QUICK_MESSAGE_TRAY_HEIGHT = 170.dp

internal object QuickMessageTokens {
    val ink = Color(0xFF0F1A2B)
    val bubble = Color(0xFFCFE0F5)
    val teal = Color(0xFF9FD8CF)
    val placeholder = Color(0xFF8E9DB3)
    val error = Color(0xFFFFB4AB)
    val content = Color(0xFFE6EDF7)
}

internal object QuickMessageTiming {
    const val bubbleDelayMs = 100L
    const val receiptDelayMs = 120L
    const val confirmationHoldMs = 700L
    const val arrowDurationMs = 450
    const val failureReturnDurationMs = 200
    const val receiptDurationMs = 280
}
