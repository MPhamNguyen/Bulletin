package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
    val descriptionModifier = homeDescriptionModifier(descriptionOverflows, expanded, onToggle)
    Row(
        modifier = descriptionModifier,
        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = description,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = BulletinFeedCardDefaults.secondaryContent,
            maxLines = if (expanded) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = {
                if (!expanded) descriptionOverflows = it.hasVisualOverflow
            }
        )
        if (descriptionOverflows) {
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = BulletinFeedCardDefaults.secondaryContent,
                modifier = Modifier.padding(top = 2.dp).size(18.dp).rotate(chevronRotation)
            )
        }
    }
}

private fun homeDescriptionModifier(descriptionOverflows: Boolean, expanded: Boolean, onToggle: () -> Unit): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(top = BulletinSpacing.xs)
        .heightIn(min = 48.dp)
        .then(
            if (descriptionOverflows) {
                Modifier
                    .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (expanded) "Collapse description" else "Expand description",
                        onClick = onToggle
                    )
            } else {
                Modifier
            }
        )
        .padding(vertical = 10.dp)
