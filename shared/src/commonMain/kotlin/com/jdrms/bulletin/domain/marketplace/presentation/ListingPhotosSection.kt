package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.jdrms.bulletin.core.common.cropSettings
import com.jdrms.bulletin.core.common.imageRequestUrl
import com.jdrms.bulletin.core.designsystem.skeleton
import kotlinx.coroutines.launch

@Composable
internal fun ListingPhotosSection(photos: List<String>, isLoading: Boolean = false) {
    if (photos.isEmpty()) {
        Box(
            modifier = Modifier
                .skeleton(isLoading, shape = MaterialTheme.shapes.medium)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = "No photos",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "No photos uploaded for this listing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        val pagerState = rememberPagerState(pageCount = { photos.size })
        val coroutineScope = rememberCoroutineScope()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    key = { page -> photos[page] }
                ) { page ->
                    val crop = photos[page].cropSettings()
                    SubcomposeAsyncImage(
                        model = photos[page].imageRequestUrl(),
                        contentDescription = "Listing photo ${page + 1} of ${photos.size}",
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        contentScale = ContentScale.Fit,
                        loading = {
                            Box(Modifier.fillMaxWidth().aspectRatio(1f).skeleton(visible = true))
                        },
                        error = { ListingPhotoFallback() },
                        success = {
                            SubcomposeAsyncImageContent(
                                Modifier.graphicsLayer {
                                    scaleX = crop.scale
                                    scaleY = crop.scale
                                    translationX = -crop.offsetX * size.width * crop.scale
                                    translationY = -crop.offsetY * size.height * crop.scale
                                }
                            )
                        }
                    )
                }
                if (photos.size > 1) {
                    if (pagerState.currentPage > 0) {
                        PhotoCarouselArrow(
                            isPrevious = true,
                            modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp),
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        )
                    }
                    if (pagerState.currentPage < photos.lastIndex) {
                        PhotoCarouselArrow(
                            isPrevious = false,
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        )
                    }
                }
            }
            if (photos.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    photos.indices.forEach { page ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (pagerState.currentPage == page) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == page) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoCarouselArrow(
    isPrevious: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPrevious) Icons.Outlined.ChevronLeft else Icons.Outlined.ChevronRight,
            contentDescription = if (isPrevious) "Previous photo" else "Next photo",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun ListingPhotoFallback() {
    Box(
        modifier = Modifier.fillMaxWidth().height(240.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
