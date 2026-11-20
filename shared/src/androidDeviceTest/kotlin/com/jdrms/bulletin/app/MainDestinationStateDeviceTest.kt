package com.jdrms.bulletin.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdrms.bulletin.app.navigation.AppDestination
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class MainDestinationStateDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun marketplaceScrollSurvivesEveryOtherDestination() {
        val destination = mutableStateOf(AppDestination.MARKETPLACE)
        lateinit var grid: LazyGridState
        compose.setContent {
            MainDestinationState(destination.value) {
                if (destination.value == AppDestination.MARKETPLACE) {
                    grid = rememberLazyGridState()
                    LazyVerticalGrid(GridCells.Fixed(2), Modifier.testTag("market"), state = grid) {
                        items(100) { Box(Modifier.height(101.dp)) }
                    }
                }
            }
        }
        compose.onNodeWithTag("market").performScrollToIndex(40)
        var index = 0
        var offset = 0
        compose.runOnIdle {
            index = grid.firstVisibleItemIndex
            offset = grid.firstVisibleItemScrollOffset
            assertTrue(index > 0)
        }
        AppDestination.entries.filter { it != AppDestination.MARKETPLACE }.forEach { other ->
            compose.runOnIdle { destination.value = other }
            compose.waitForIdle()
            compose.onNodeWithTag("market").assertDoesNotExist()
            compose.runOnIdle { destination.value = AppDestination.MARKETPLACE }
            compose.waitForIdle()
            compose.runOnIdle {
                assertEquals(index, grid.firstVisibleItemIndex, "Returning from $other")
                assertEquals(offset, grid.firstVisibleItemScrollOffset, "Returning from $other")
            }
        }
    }
}
