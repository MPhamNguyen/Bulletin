package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme

@Composable
private fun DeckSkeletonPreview(dark: Boolean) {
    BulletinTheme(darkTheme = dark) {
        HomeDeckSkeleton(Modifier.width(366.dp).height(640.dp))
    }
}

@Preview
@Composable
internal fun HomeDeckSkeletonLightPreview() = DeckSkeletonPreview(dark = false)

@Preview
@Composable
internal fun HomeDeckSkeletonDarkPreview() = DeckSkeletonPreview(dark = true)
