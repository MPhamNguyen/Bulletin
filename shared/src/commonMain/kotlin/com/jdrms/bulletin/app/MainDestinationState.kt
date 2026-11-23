package com.jdrms.bulletin.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.jdrms.bulletin.app.navigation.AppDestination

/** Retains saveable UI state, including lazy scroll positions, while a tab is off screen. */
@Composable
internal fun MainDestinationState(destination: AppDestination, content: @Composable () -> Unit) {
    val stateHolder = rememberSaveableStateHolder()
    stateHolder.SaveableStateProvider(destination.name, content)
}
