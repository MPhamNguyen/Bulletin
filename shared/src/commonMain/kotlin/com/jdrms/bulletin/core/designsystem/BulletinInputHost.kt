package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.toSize

private class InputFocus {
    var coordinates: LayoutCoordinates? = null
}

private class InputHostState {
    var focused: InputFocus? = null
    var coordinates: LayoutCoordinates? = null

    fun contains(position: Offset): Boolean {
        val host = coordinates?.takeIf { it.isAttached } ?: return true
        val input = focused?.coordinates?.takeIf { it.isAttached } ?: return true
        return Rect(host.localPositionOf(input, Offset.Zero), input.size.toSize()).contains(position)
    }
}

private val LocalInputHost = staticCompositionLocalOf<InputHostState?> { null }

/** Install once per input surface, including separate dialog and bottom-sheet windows. */
@Composable
fun BulletinInputHost(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = remember { InputHostState() }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    CompositionLocalProvider(LocalInputHost provides state) {
        Box(
            modifier.onGloballyPositioned { state.coordinates = it }
                .pointerInput(state, focus, keyboard) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        if (state.focused != null && !state.contains(down.position)) {
                            focus.clearFocus()
                            keyboard?.hide()
                        }
                    }
                },
            propagateMinConstraints = true
        ) { content() }
    }
}

@Composable
internal fun Modifier.bulletinInputFocus(): Modifier {
    val host = LocalInputHost.current
    val input = remember { InputFocus() }
    DisposableEffect(host, input) {
        onDispose { if (host?.focused === input) host.focused = null }
    }
    return onGloballyPositioned { input.coordinates = it }
        .onFocusChanged {
            if (it.isFocused) {
                host?.focused = input
            } else if (host?.focused === input) {
                host.focused = null
            }
        }
}
