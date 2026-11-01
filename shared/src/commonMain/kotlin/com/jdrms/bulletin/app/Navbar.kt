package com.jdrms.bulletin.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.core.designsystem.theme.BulletinMotion
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

private val NavigationIconSize = 30.dp

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun Navbar(
    currentDestination: AppDestination,
    isVisible: () -> Boolean,
    onDestinationSelected: (AppDestination) -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
    profileIconModifier: Modifier = Modifier
) {
    val navbarIsVisible = isVisible() && !WindowInsets.isImeVisible
    val hiddenFraction = animateFloatAsState(
        targetValue = if (navbarIsVisible) 0f else 1f,
        animationSpec = tween(durationMillis = BulletinMotion.NAVBAR),
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
            .padding(horizontal = BulletinSpacing.m, vertical = BulletinSpacing.s)
            .fillMaxWidth()
            .semantics { if (!navbarIsVisible) hideFromAccessibility() },
        contentAlignment = Alignment.Center
    ) {
        NavbarContent(currentDestination, onDestinationSelected, profileIconModifier)
    }
}

@Composable
private fun NavbarContent(
    currentDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
    profileIconModifier: Modifier
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
                        profileIconModifier = profileIconModifier,
                        selected = currentDestination == destination,
                        modifier = Modifier.weight(1f),
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
    profileIconModifier: Modifier,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val itemColor = animateColorAsState(
        targetValue = if (selected) colorScheme.primary.copy(alpha = 0.2f) else colorScheme.surfaceContainer,
        animationSpec = tween(durationMillis = BulletinMotion.STANDARD),
        label = "navigation item background"
    )
    Surface(
        onClick = onClick,
        modifier = modifier.height(60.dp).semantics {
            contentDescription = destination.label
            this.selected = selected
            role = Role.Tab
        },
        shape = CircleShape,
        color = itemColor.value
    ) {
        Box(contentAlignment = Alignment.Center) {
            DestinationIcon(destination, selected, profileIconModifier)
        }
    }
}

@Composable
private fun DestinationIcon(destination: AppDestination, selected: Boolean, profileIconModifier: Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    // The taller house silhouette needs a smaller box to match the storefront's visible height.
    val iconSize = if (destination == AppDestination.HOME) NavigationIconSize - 2.dp else NavigationIconSize
    val contentColor = animateColorAsState(
        targetValue = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = BulletinMotion.STANDARD),
        label = "navigation item content"
    )
    val iconScale = animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = tween(durationMillis = BulletinMotion.STANDARD),
        label = "navigation item icon scale"
    )
    Icon(
        imageVector = if (selected) destination.selectedIcon else destination.icon,
        contentDescription = null,
        tint = contentColor.value,
        modifier = Modifier.size(iconSize)
            .then(if (destination == AppDestination.PROFILE) profileIconModifier else Modifier).graphicsLayer {
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
