package com.jdrms.bulletin.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.app.navigation.AppDestination
import kotlinx.coroutines.launch

private val NavigationIconSize = 30.dp

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun Navbar(
    currentDestination: AppDestination,
    isVisible: () -> Boolean,
    onDestinationSelected: (AppDestination) -> Unit,
    onFocused: () -> Unit,
    onDestinationPositioned: (AppDestination, Offset) -> Unit = { _, _ -> },
    profileSaveTrigger: Int = 0,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val navbarIsVisible = isVisible() && !WindowInsets.isImeVisible
    val hiddenFraction = animateFloatAsState(
        targetValue = if (navbarIsVisible) 0f else 1f,
        animationSpec = tween(durationMillis = 250),
        label = "navbar visibility"
    )
    Box(
        modifier = modifier
            .onFocusChanged { if (it.hasFocus) onFocused() }
            .focusGroup()
            .graphicsLayer {
                translationY = size.height * hiddenFraction.value
                alpha = 1f - hiddenFraction.value
            }
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .semantics { if (!navbarIsVisible) hideFromAccessibility() },
        contentAlignment = Alignment.Center
    ) {
        NavbarContent(currentDestination, onDestinationSelected, onDestinationPositioned, profileSaveTrigger, reducedMotion)
    }
}

@Composable
private fun NavbarContent(
    currentDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
    onDestinationPositioned: (AppDestination, Offset) -> Unit,
    profileSaveTrigger: Int,
    reducedMotion: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp).padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppDestination.entries.forEach { destination ->
                if (destination == AppDestination.LISTINGS) {
                    CreateListingItem(
                        modifier = Modifier.weight(1f),
                        onClick = { onDestinationSelected(destination) }
                    )
                } else {
                    DestinationItem(
                        destination = destination,
                        selected = currentDestination == destination,
                        modifier = Modifier.weight(1f),
                        onPositioned = { onDestinationPositioned(destination, it) },
                        saveTrigger = if (destination == AppDestination.PROFILE) profileSaveTrigger else 0,
                        reducedMotion = reducedMotion,
                        onClick = { onDestinationSelected(destination) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationItem(
    destination: AppDestination,
    selected: Boolean,
    modifier: Modifier,
    saveTrigger: Int,
    reducedMotion: Boolean,
    onPositioned: (Offset) -> Unit,
    onClick: () -> Unit
) {
    val bump = androidx.compose.runtime.remember { Animatable(1f) }
    val glow = androidx.compose.runtime.remember { Animatable(0f) }
    LaunchedEffect(saveTrigger) {
        if (saveTrigger > 0) {
            kotlinx.coroutines.delay(if (reducedMotion) 180 else 560)
            bump.snapTo(1f)
            glow.snapTo(0f)
            if (reducedMotion) {
                bump.animateTo(1.12f, tween(100))
                bump.animateTo(1f, tween(120))
                return@LaunchedEffect
            }
            kotlinx.coroutines.coroutineScope {
                launch { bump.animateTo(1.25f, tween(170)); bump.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 420f)) }
                launch { glow.animateTo(1f, tween(600)); glow.snapTo(0f) }
            }
        }
    }
    val colorScheme = MaterialTheme.colorScheme
    val itemColor = animateColorAsState(
        targetValue = if (selected) colorScheme.primary.copy(alpha = 0.2f) else colorScheme.surfaceContainer,
        animationSpec = tween(durationMillis = 220),
        label = "navigation item background"
    )
    Surface(
        onClick = onClick,
        modifier = modifier.height(60.dp).graphicsLayer { scaleX = bump.value; scaleY = bump.value }
            .onGloballyPositioned { coordinates ->
            val position = coordinates.positionInRoot()
            onPositioned(Offset(position.x + coordinates.size.width / 2f, position.y + coordinates.size.height / 2f))
        }.semantics {
            contentDescription = destination.label
            this.selected = selected
            role = Role.Tab
        },
        shape = CircleShape,
        color = itemColor.value
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (destination == AppDestination.PROFILE && glow.value > 0f) {
                Canvas(Modifier.size(54.dp)) {
                    drawCircle(Color(0xFF4FAE9F).copy(alpha = (1f - glow.value) * 0.55f),
                        radius = size.minDimension * (0.38f + glow.value * 0.26f),
                        style = Stroke(2.dp.toPx()))
                }
            }
            DestinationIcon(destination, selected)
        }
    }
}

@Composable
private fun DestinationIcon(destination: AppDestination, selected: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    // The taller house silhouette needs a smaller box to match the storefront's visible height.
    val iconSize = if (destination == AppDestination.HOME) NavigationIconSize - 2.dp else NavigationIconSize
    val contentColor = animateColorAsState(
        targetValue = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 220),
        label = "navigation item content"
    )
    val iconScale = animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "navigation item icon scale"
    )
    Icon(
        imageVector = if (selected) destination.selectedIcon else destination.icon,
        contentDescription = null,
        tint = contentColor.value,
        modifier = Modifier.size(iconSize).graphicsLayer {
            scaleX = iconScale.value
            scaleY = iconScale.value
        }
    )
}

@Composable
private fun CreateListingItem(modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(60.dp)
            .semantics {
                contentDescription = "Create listing"
                role = Role.Tab
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(NavigationIconSize),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}
