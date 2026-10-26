package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinFeedCardDefaults
import com.jdrms.bulletin.core.designsystem.BulletinTheme

@Composable
private fun FeedbackPreview(dark: Boolean, save: Float, pass: Float) {
    BulletinTheme(darkTheme = dark) {
        Box(Modifier.size(350.dp, 450.dp).background(BulletinFeedCardDefaults.backgroundTop)) {
            HomeSwipeFeedback({ save }, { pass })
        }
    }
}

@Preview
@Composable
internal fun SaveLightPreview() = FeedbackPreview(false, 1f, 0f)

@Preview
@Composable
internal fun SaveDarkPreview() = FeedbackPreview(true, 1f, 0f)

@Preview
@Composable
internal fun PassLightPreview() = FeedbackPreview(false, 0f, 1f)

@Preview
@Composable
internal fun PassDarkPreview() = FeedbackPreview(true, 0f, 1f)
