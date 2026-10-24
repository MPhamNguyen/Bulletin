package com.jdrms.bulletin.domain.home.presentation

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal actual fun rememberHomeMotionDurationScale(): Float {
    if (LocalInspectionMode.current) return 1f
    val resolver = LocalContext.current.contentResolver
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var scale by remember(resolver) { mutableFloatStateOf(readHomeMotionScale(resolver)) }
    DisposableEffect(resolver, lifecycle) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { scale = readHomeMotionScale(resolver) }
        }
        val resume = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) scale = readHomeMotionScale(resolver)
        }
        val registered = runCatching {
            resolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                false,
                observer
            )
        }.isSuccess
        lifecycle.addObserver(resume)
        scale = readHomeMotionScale(resolver)
        onDispose {
            if (registered) resolver.unregisterContentObserver(observer)
            lifecycle.removeObserver(resume)
        }
    }
    return scale
}

private fun readHomeMotionScale(resolver: ContentResolver): Float = homeMotionDurationScale(
    runCatching { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }.getOrNull()
)
