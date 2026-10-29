package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/** Apply to each window hosting inputs. Consumed taps and drags leave focus alone. */
@Composable
fun Modifier.dismissKeyboardOnTapOutside(): Modifier {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return pointerInput(focusManager, keyboard) {
        detectTapGestures {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }
}

/** Observe taps without consuming the field's gestures; never raise IME for noneditable inputs. */
@Composable
internal fun Modifier.showKeyboardOnFocusOrTap(enabled: Boolean, readOnly: Boolean): Modifier {
    val keyboard = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }
    val canType = enabled && !readOnly
    return onFocusChanged {
        focused = it.isFocused
        if (focused && canType) keyboard?.show()
    }.pointerInput(canType, keyboard) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
            if (up != null && focused && canType) keyboard?.show()
        }
    }
}
