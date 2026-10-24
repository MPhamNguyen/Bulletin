package com.jdrms.bulletin.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs

internal class NavbarScrollBehavior(private val thresholdPx: Float) {
    var isVisible by mutableStateOf(true)
        private set

    private var lastDirection = 0
    private var accumulatedDistance = 0f

    init {
        require(thresholdPx > 0f)
    }

    fun onScrollDelta(delta: Float) {
        if (delta == 0f) return

        val direction = if (delta > 0f) 1 else -1
        if (direction != lastDirection) {
            accumulatedDistance = 0f
            lastDirection = direction
        }
        accumulatedDistance += abs(delta)

        if (accumulatedDistance >= thresholdPx) {
            isVisible = direction < 0
            accumulatedDistance = 0f
        }
    }

    fun reveal() {
        isVisible = true
        accumulatedDistance = 0f
        lastDirection = 0
    }

    fun hide() {
        isVisible = false
        accumulatedDistance = 0f
        lastDirection = 0
    }

    fun onDestinationChanged() = reveal()
}
