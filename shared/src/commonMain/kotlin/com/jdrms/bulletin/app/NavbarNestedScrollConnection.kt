package com.jdrms.bulletin.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

internal class NavbarNestedScrollConnection(
    private val behavior: NavbarScrollBehavior
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        behavior.onScrollDelta(-available.y)
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (available.y > 0f && consumed.y == 0f) behavior.reveal()
        return Offset.Zero
    }
}
