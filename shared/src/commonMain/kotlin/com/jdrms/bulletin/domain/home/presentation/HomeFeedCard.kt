package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinFeedCardDefaults
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import com.jdrms.bulletin.domain.home.presentation.quickmessage.QUICK_MESSAGE_TRAY_HEIGHT
import com.jdrms.bulletin.domain.home.presentation.quickmessage.QuickMessageActions
import com.jdrms.bulletin.domain.home.presentation.quickmessage.QuickMessageTray

internal data class HomeFeedCardState(
    val isActive: Boolean = true,
    val expansionProgress: Float = 0f,
    val quickSendTrigger: Int = 0,
    val passProgress: Float = 0f,
    val saveProgress: Float = 0f
)

@Composable
internal fun HomeFeedCard(
    item: HomeFeedItem,
    modifier: Modifier = Modifier,
    state: HomeFeedCardState = HomeFeedCardState(),
    actions: QuickMessageActions = QuickMessageActions({ false }, {}, {}, {})
) {
    val photos = rememberHomePhotoPlayback(item, state.isActive, state.expansionProgress > 0f)
    Box(
        modifier.fillMaxSize().padding(8.dp).shadow(8.dp, MaterialTheme.shapes.large)
            .clip(MaterialTheme.shapes.large).background(BulletinFeedCardDefaults.cardSurface)
            .homePhotoHold(photos)
    ) {
        HomeFeedMedia(item, photos, state.expansionProgress)
        if (state.passProgress > 0f) {
            HomePassTint(state.passProgress, Modifier.matchParentSize())
            HomePassStamp(state.passProgress, Modifier.align(Alignment.TopEnd))
        }
        if (state.saveProgress > 0f) {
            HomeSaveTint(state.saveProgress, Modifier.matchParentSize())
            HomeSaveStamp(state.saveProgress, Modifier.align(Alignment.TopStart))
        }
        if (state.expansionProgress > 0f) {
            HomeTrayBackground(state.expansionProgress, Modifier.align(Alignment.BottomCenter))
        }
        if (state.isActive && state.expansionProgress > 0.3f) {
            QuickMessageTray(
                listingId = item.id.value,
                visibleProgress = state.expansionProgress,
                quickSendTrigger = state.quickSendTrigger,
                actions = actions,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun BoxScope.HomeFeedMedia(item: HomeFeedItem, photos: HomePhotoPlayback, expansionProgress: Float) {
    val density = LocalDensity.current
    var descriptionExpanded by rememberSaveable(item.id.value) { mutableStateOf(false) }
    Box(Modifier.matchParentSize().clipToBounds()) {
        Box(
            Modifier.matchParentSize().graphicsLayer {
                translationY = -expansionProgress * with(density) { QUICK_MESSAGE_TRAY_HEIGHT.toPx() }
            }
        ) {
            HomeFeedCardBackground(
                item = item,
                photoUrl = photos.urls.getOrNull(photos.index),
                photoIndex = photos.index,
                photoCount = photos.urls.size,
                photoProgress = photos.progress.value,
                onPreviousPhoto = { photos.step(-1) },
                onNextPhoto = { photos.step(1) }
            )
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                HomeFeedDetails(item)
                HomeFeedDescription(
                    item.description,
                    descriptionExpanded
                ) { descriptionExpanded = !descriptionExpanded }
            }
        }
    }
}

@Composable
private fun HomeTrayBackground(progress: Float, modifier: Modifier) {
    Box(
        modifier.fillMaxWidth().height(QUICK_MESSAGE_TRAY_HEIGHT)
            .graphicsLayer { alpha = progress }
            .background(
                Brush.verticalGradient(
                    0f to BulletinFeedCardDefaults.cardSurface,
                    0.08f to lerp(
                        BulletinFeedCardDefaults.cardSurface, MaterialTheme.colorScheme.surfaceContainerLow, 0.22f
                    ),
                    0.18f to lerp(
                        BulletinFeedCardDefaults.cardSurface, MaterialTheme.colorScheme.surfaceContainerLow, 0.58f
                    ),
                    0.28f to MaterialTheme.colorScheme.surfaceContainerLow,
                    1f to MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
    )
}
