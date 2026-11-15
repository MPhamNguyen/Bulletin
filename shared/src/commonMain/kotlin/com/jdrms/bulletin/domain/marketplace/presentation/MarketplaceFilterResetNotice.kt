package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinElevation
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import kotlinx.coroutines.delay

/** Inverse-colored bar shown briefly after a category change cleared other filters. */
@Composable
internal fun MarketplaceFilterResetNotice(
    restartKey: Any,
    onUndo: () -> Unit,
    onExpired: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(restartKey) {
        delay(NOTICE_DURATION_MILLIS)
        onExpired()
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = BulletinShapes.inset,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = BulletinElevation.raised
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filters reset for this category",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Undo",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onUndo).padding(12.dp)
            )
        }
    }
}

private const val NOTICE_DURATION_MILLIS = 5_000L
