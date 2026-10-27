package com.jdrms.bulletin.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/** App-owned geometry bridge; Home receives only window coordinates, never a navigation destination. */
internal class ProfileTabAnchor {
    private data class Position(val coordinates: LayoutCoordinates, val measuredBounds: Rect)
    private var position by mutableStateOf<Position?>(null)
    val modifier: Modifier = Modifier.onGloballyPositioned { position = Position(it, it.boundsInWindow()) }

    fun bounds(visible: Boolean): Rect? = if (visible) {
        position?.coordinates?.takeIf { it.isAttached }?.boundsInWindow()?.takeIf { it.width > 0f && it.height > 0f }
    } else {
        null
    }
}
