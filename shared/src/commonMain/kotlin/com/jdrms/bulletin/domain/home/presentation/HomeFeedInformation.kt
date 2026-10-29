package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinFeedCardDefaults
import com.jdrms.bulletin.core.designsystem.BulletinSpacing
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCategory
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import kotlin.math.min

@Composable
internal fun HomeFeedInformation(item: HomeFeedItem, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier) {
    val scroll = rememberScrollState()
    val fling = ScrollableDefaults.flingBehavior()
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home-description-${item.id.value}")
            .homeDescriptionScroll(expanded, scroll, fling, scope)
            .verticalScroll(scroll, enabled = false)
            .padding(horizontal = 18.dp, vertical = BulletinSpacing.l)
    ) {
        HomeFeedDetails(item)
        HomeFeedDescription(description = item.description, expanded = expanded, onToggle = onToggle)
    }
}

@Composable
private fun HomeFeedDetails(item: HomeFeedItem) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.headlineMedium,
            color = BulletinFeedCardDefaults.content
        )
        Text(
            text = item.price.formatted,
            style = MaterialTheme.typography.titleLarge,
            color = BulletinFeedCardDefaults.content
        )
        HomeFeedCategoryPill(item.category, modifier = Modifier.padding(top = BulletinSpacing.xs))
    }
}

@Composable
private fun HomeFeedCategoryPill(category: HomeFeedCategory, modifier: Modifier = Modifier) {
    val icon = when (category) {
        HomeFeedCategory.TEXTBOOKS -> Icons.AutoMirrored.Outlined.MenuBook
        HomeFeedCategory.ELECTRONICS -> Icons.Outlined.Keyboard
        HomeFeedCategory.FURNITURE -> Icons.Outlined.Chair
        HomeFeedCategory.CLOTHING -> Icons.Outlined.Checkroom
        HomeFeedCategory.HOUSING -> Icons.Outlined.Home
        HomeFeedCategory.OTHER -> Icons.Outlined.Category
    }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = BulletinFeedCardDefaults.content.copy(alpha = 0.20f),
        contentColor = BulletinFeedCardDefaults.content,
        border = BorderStroke(1.dp, BulletinFeedCardDefaults.content.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(
                start = BulletinSpacing.s,
                end = 10.dp,
                top = BulletinSpacing.xs,
                bottom = BulletinSpacing.xs
            ),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(
                text = category.name.lowercase().replaceFirstChar { it.titlecase() },
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun HomeFeedDescription(description: String, expanded: Boolean, onToggle: () -> Unit) {
    if (description.isBlank()) return
    var descriptionOverflows by remember(description) { mutableStateOf(false) }
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
                modifier = Modifier.padding(top = 2.dp).size(18.dp).rotate(if (expanded) 180f else 0f)
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
