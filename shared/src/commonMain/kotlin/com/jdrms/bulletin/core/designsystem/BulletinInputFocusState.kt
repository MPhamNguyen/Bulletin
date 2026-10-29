package com.jdrms.bulletin.core.designsystem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Tracks input focus across the app surface and its dialog or sheet hosts. */
internal class BulletinInputFocusState {
    private var focusedInputs by mutableStateOf(emptySet<Any>())
    val isFocused: Boolean get() = focusedInputs.isNotEmpty()

    fun update(input: Any, focused: Boolean) {
        focusedInputs = if (focused) focusedInputs + input else focusedInputs - input
    }
}

internal val LocalBulletinInputFocus = staticCompositionLocalOf<BulletinInputFocusState?> { null }
