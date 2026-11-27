package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinFeedCardDefaults
import com.jdrms.bulletin.core.designsystem.theme.BulletinMotion
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun HomeFeedDescription(description: String, expanded: Boolean, onToggle: () -> Unit) {
    if (description.isBlank()) return
    var descriptionOverflows by remember(description) { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(BulletinMotion.PULSE, easing = FastOutSlowInEasing)
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = BulletinSpacing.xs).heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.xs),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = description,
            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = BulletinFeedCardDefaults.secondaryContent,
            maxLines = if (expanded) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = {
                if (!expanded) descriptionOverflows = it.hasVisualOverflow
            }
        )
        if (descriptionOverflows) {
            HomeDescriptionChevron(expanded, chevronRotation, onToggle)
        }
    }
}

/** The only tappable part of the description: everything else passes taps to the photo zones. */
@Composable
private fun HomeDescriptionChevron(expanded: Boolean, rotation: Float, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .semantics {
                contentDescription = if (expanded) "Collapse description" else "Expand description"
                stateDescription = if (expanded) "Expanded" else "Collapsed"
            }
            .clickable(role = Role.Button, onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = BulletinFeedCardDefaults.secondaryContent,
            modifier = Modifier.size(18.dp).rotate(rotation)
        )
    }
}
