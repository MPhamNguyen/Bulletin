package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinFeedCardDefaults
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCategory
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import com.jdrms.bulletin.domain.home.presentation.quickmessage.QuickMessageActions
import com.jdrms.bulletin.domain.home.presentation.quickmessage.QuickMessageTray
import com.jdrms.bulletin.domain.home.presentation.quickmessage.QUICK_MESSAGE_TRAY_HEIGHT
import kotlin.math.max
import kotlin.math.min

private const val HOME_PHOTO_INTERVAL_MILLIS = 3_000L

@Composable
fun HomeFeedCard(
    item: HomeFeedItem,
    modifier: Modifier = Modifier,
    cardIndex: Int = 0,
    isActive: Boolean = true,
    expansionProgress: Float = 0f,
    onSendQuickMessage: suspend (String) -> Boolean = { false },
    onMessageSending: () -> Unit = {},
    onMessageSendFailed: () -> Unit = {},
    onMessageAnimationComplete: () -> Unit = {}
) {
    val density = LocalDensity.current
    var descriptionExpanded by rememberSaveable(item.id.value, cardIndex) { mutableStateOf(false) }
    val photoUrls = remember(item.photoUrls, item.imageUrl) {
        item.photoUrls.ifEmpty { listOfNotNull(item.imageUrl) }
    }
    var photoIndex by rememberSaveable(item.id.value, cardIndex, photoUrls) { mutableIntStateOf(0) }
    var isHolding by remember(item.id.value, cardIndex) { mutableStateOf(false) }
    var isLongPress by remember(item.id.value, cardIndex) { mutableStateOf(false) }
    val photoProgress = remember(item.id.value, photoUrls, photoIndex) { Animatable(0f) }
    LaunchedEffect(item.id.value, photoUrls, isActive, photoIndex, isHolding) {
        if (isActive && !isHolding && photoUrls.size > 1) {
            photoProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (HOME_PHOTO_INTERVAL_MILLIS * (1f - photoProgress.value))
                        .toInt()
                        .coerceAtLeast(1),
                    easing = LinearEasing
                )
            )
            if (photoIndex < photoUrls.lastIndex) photoIndex++
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
            .clip(MaterialTheme.shapes.large)
            .background(BulletinFeedCardDefaults.cardSurface)
            .pointerInput(item.id.value, cardIndex) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isHolding = true
                    isLongPress = false
                    try {
                        val longPress = awaitLongPressOrCancellation(down.id)
                        isLongPress = longPress != null
                        if (longPress != null) {
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Final)
                            } while (event.changes.any { it.pressed })
                        }
                    } finally {
                        isHolding = false
                    }
                }
            }
    ) {
        // Keep the shifted media and listing content inside the card's original viewport.
        Box(Modifier.matchParentSize().clipToBounds()) {
            Box(Modifier.matchParentSize().graphicsLayer {
                translationY = -expansionProgress * with(density) { QUICK_MESSAGE_TRAY_HEIGHT.toPx() }
            }) {
                HomeFeedCardBackground(
                    item = item,
                    photoUrl = photoUrls.getOrNull(photoIndex),
                    photoIndex = photoIndex,
                    photoCount = photoUrls.size,
                    photoProgress = photoProgress.value,
                    onPreviousPhoto = {
                        if (!isLongPress) photoIndex = stepHomePhotoIndex(photoIndex, -1, photoUrls.size)
                    },
                    onNextPhoto = {
                        if (!isLongPress) photoIndex = stepHomePhotoIndex(photoIndex, 1, photoUrls.size)
                    }
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    HomeFeedDetails(item)
                    HomeFeedDescription(
                        description = item.description,
                        expanded = descriptionExpanded,
                        onToggle = { descriptionExpanded = !descriptionExpanded }
                    )
                }
            }
        }
        if (expansionProgress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(QUICK_MESSAGE_TRAY_HEIGHT)
                    .graphicsLayer { alpha = expansionProgress }
                    .background(
                        Brush.verticalGradient(
                            0f to BulletinFeedCardDefaults.cardSurface,
                            0.08f to lerp(
                                BulletinFeedCardDefaults.cardSurface,
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                0.22f
                            ),
                            0.18f to lerp(
                                BulletinFeedCardDefaults.cardSurface,
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                0.58f
                            ),
                            0.28f to MaterialTheme.colorScheme.surfaceContainerLow,
                            1f to MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
            )
        }
        if (isActive && expansionProgress > 0.3f) {
            QuickMessageTray(
                listingId = item.id.value,
                visibleProgress = expansionProgress,
                actions = QuickMessageActions(
                    onSend = onSendQuickMessage,
                    onSending = onMessageSending,
                    onSendFailed = onMessageSendFailed,
                    onAnimationComplete = onMessageAnimationComplete
                ),
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

internal fun stepHomePhotoIndex(currentIndex: Int, direction: Int, photoCount: Int): Int {
    if (photoCount <= 0) return 0
    return (currentIndex + direction).coerceIn(0, photoCount - 1)
}

internal fun cropAxisTranslation(
    offset: Float,
    scale: Float,
    frameExtent: Float,
    referenceSide: Float,
    fittedExtent: Float
): Float {
    val raw = -offset * referenceSide * scale
    val maxTranslation = max(0f, (fittedExtent * scale - frameExtent) / 2f)
    return raw.coerceIn(-maxTranslation, maxTranslation)
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
        HomeFeedCategoryPill(item.category, modifier = Modifier.padding(top = 4.dp))
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
            modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
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
    val descriptionModifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp)
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
    Row(
        modifier = descriptionModifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                if (!expanded) descriptionOverflows = it.didOverflowWidth
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
