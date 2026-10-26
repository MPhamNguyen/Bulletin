package com.jdrms.bulletin.domain.home.presentation

import android.content.Context
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

@Composable
internal actual fun HomePlatformAccessibilityFocus(targetTag: String?) {
    val view = LocalView.current
    val manager = LocalContext.current.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    LaunchedEffect(targetTag, view) {
        if (targetTag == null || !manager.isEnabled || !manager.isTouchExplorationEnabled) return@LaunchedEffect
        // Semantics and the platform virtual-node tree are published after layout.
        repeat(3) {
            withFrameNanos { }
            if (requestHomeAccessibilityFocus(view.createAccessibilityNodeInfo(), targetTag)) return@LaunchedEffect
        }
    }
}

private fun requestHomeAccessibilityFocus(root: AccessibilityNodeInfo?, tag: String): Boolean {
    if (root == null) return false
    if (root.viewIdResourceName == tag) {
        return root.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
    }
    for (index in 0 until root.childCount) {
        if (requestHomeAccessibilityFocus(root.getChild(index), tag)) return true
    }
    return false
}
