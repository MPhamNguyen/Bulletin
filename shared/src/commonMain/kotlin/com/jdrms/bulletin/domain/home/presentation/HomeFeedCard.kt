package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinFeedCardDefaults
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCategory
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent

private const val HOME_PHOTO_INTERVAL_MILLIS = 3_000L

@Composable
fun HomeFeedCard(
    item: HomeFeedItem,
    modifier: Modifier = Modifier,
    cardIndex: Int = 0,
    isActive: Boolean = true
) {
    var descriptionExpanded by rememberSaveable(item.id.value, cardIndex) { mutableStateOf(false) }
    val photoUrls = remember(item.photoUrls, item.imageUrl) {
        item.photoUrls.ifEmpty { listOfNotNull(item.imageUrl) }
    }
    var photoIndex by rememberSaveable(item.id.value, cardIndex, photoUrls) { mutableIntStateOf(0) }
    val photoProgress = remember(item.id.value, photoUrls, photoIndex) { Animatable(0f) }
    LaunchedEffect(item.id.value, photoUrls, isActive, photoIndex) {
        photoProgress.snapTo(0f)
        if (isActive && photoUrls.size > 1) {
            photoProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = HOME_PHOTO_INTERVAL_MILLIS.toInt(),
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
    ) {
        HomeFeedCardBackground(
            item = item,
            photoUrl = photoUrls.getOrNull(photoIndex),
            photoIndex = photoIndex,
            photoCount = photoUrls.size,
            photoProgress = photoProgress.value,
            onPreviousPhoto = { photoIndex = stepHomePhotoIndex(photoIndex, -1, photoUrls.size) },
            onNextPhoto = { photoIndex = stepHomePhotoIndex(photoIndex, 1, photoUrls.size) }
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

internal fun stepHomePhotoIndex(currentIndex: Int, direction: Int, photoCount: Int): Int {
    if (photoCount <= 0) return 0
    return (currentIndex + direction).coerceIn(0, photoCount - 1)
}

@Composable
private fun BoxScope.HomeFeedCardBackground(
    item: HomeFeedItem,
    photoUrl: String?,
    photoIndex: Int,
    photoCount: Int,
    photoProgress: Float,
    onPreviousPhoto: () -> Unit,
    onNextPhoto: () -> Unit
) {
    HomeFeedArtwork(modifier = Modifier.matchParentSize())
    photoUrl?.let { imageUrl ->
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = "${item.title} listing photo",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        ) { SubcomposeAsyncImageContent() }
    }
    HomeFeedImageScrim(Modifier.matchParentSize())
    if (photoUrl != null && photoCount > 1) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.38f)
                .fillMaxHeight()
                .clickable(onClickLabel = "Previous listing photo", onClick = onPreviousPhoto)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth(0.38f)
                .fillMaxHeight()
                .clickable(onClickLabel = "Next listing photo", onClick = onNextPhoto)
        )
        HomeFeedPhotoIndicator(
            photoCount = photoCount,
            selectedIndex = photoIndex,
            activeProgress = photoProgress,
            modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun HomeFeedPhotoIndicator(
    photoCount: Int,
    selectedIndex: Int,
    activeProgress: Float,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(photoCount) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.38f))
            ) {
                val fill = when {
                    index < selectedIndex -> 1f
                    index == selectedIndex -> activeProgress
                    else -> 0f
                }
                if (fill > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fill)
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeFeedImageScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                0f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.28f),
                0.35f to Color.Transparent,
                1f to BulletinFeedCardDefaults.scrim.copy(alpha = 0.92f)
            )
        )
    )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .heightIn(min = 48.dp)
            .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
            .clickable(
                role = Role.Button,
                onClickLabel = if (expanded) "Collapse description" else "Expand description",
                onClick = onToggle
            )
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = description,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = BulletinFeedCardDefaults.secondaryContent,
            maxLines = if (expanded) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = BulletinFeedCardDefaults.secondaryContent,
            modifier = Modifier.padding(top = 2.dp).size(18.dp).rotate(if (expanded) 180f else 0f)
        )
    }
}
