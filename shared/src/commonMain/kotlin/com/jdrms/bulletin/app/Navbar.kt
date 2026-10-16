package com.jdrms.bulletin.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.app.navigation.AppDestination

@Composable
fun Navbar(
    currentDestination: AppDestination,
    isVisible: Boolean,
    onDestinationSelected: (AppDestination) -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hiddenFraction = animateFloatAsState(
        targetValue = if (isVisible) 0f else 1f,
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .semantics { if (!isVisible) hideFromAccessibility() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(6.dp),
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
                            onClick = { onDestinationSelected(destination) }
                        )
                    }
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
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .semantics { this.selected = selected },
        shape = CircleShape,
        color = if (selected) colorScheme.primaryContainer else colorScheme.surfaceContainer,
        contentColor = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSecondaryContainer
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = destination.icon, contentDescription = null)
            Text(
                text = destination.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CreateListingItem(modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .semantics { contentDescription = "Create listing" },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
