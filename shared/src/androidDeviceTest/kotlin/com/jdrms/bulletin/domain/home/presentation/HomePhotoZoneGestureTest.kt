package com.jdrms.bulletin.domain.home.presentation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/** Exercises the real wrapper against clickable 38% zones without network-dependent photo loading. */
@RunWith(AndroidJUnit4::class)
class HomePhotoZoneGestureTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private var previous = 0
    private var next = 0
    private val releases = mutableListOf<CardGestureResolver.Release>()

    @Test
    fun tapsRemainAvailableToBothPhotoZones() {
        showZones()
        compose.onRoot().performTouchInput { click(Offset(width * 0.15f, height * 0.25f)) }
        compose.onRoot().performTouchInput { click(Offset(width * 0.85f, height * 0.25f)) }
        compose.runOnIdle {
            assertEquals(1, previous)
            assertEquals(1, next)
            assertEquals(listOf(CardGestureResolver.Release.CANCEL, CardGestureResolver.Release.CANCEL), releases)
        }
    }

    @Test
    fun dragFromPhotoZoneCancelsClickDespiteCardTransform() {
        showZones()
        compose.onRoot().performTouchInput {
            swipe(Offset(width * 0.15f, height * 0.25f), Offset(width * 0.85f, height * 0.25f))
        }
        compose.runOnIdle {
            assertEquals(0, previous)
            assertEquals(0, next)
            assertEquals(listOf(CardGestureResolver.Release.SAVE), releases)
        }
    }

    private fun showZones() {
        compose.setContent {
            val drag = remember { mutableFloatStateOf(0f) }
            val session = remember {
                HomeGestureSession(
                    interaction = { HomeInteractionState() },
                    onBegin = { drag.floatValue = 0f }, onAxis = {},
                    onDrag = { drag.floatValue = it }, onRelease = releases::add,
                    onCancel = { releases += CardGestureResolver.Release.CANCEL }
                )
            }
            Box(Modifier.fillMaxSize().homeCardGestures(session)) {
                Box(Modifier.fillMaxSize().graphicsLayer {
                    translationX = drag.floatValue * density
                    rotationZ = drag.floatValue * 0.05f
                }) {
                    Box(Modifier.align(Alignment.TopStart).fillMaxWidth(0.38f).fillMaxHeight()
                        .clickable { previous++ })
                    Box(Modifier.align(Alignment.TopEnd).fillMaxWidth(0.38f).fillMaxHeight()
                        .clickable { next++ })
                }
            }
        }
    }
}
