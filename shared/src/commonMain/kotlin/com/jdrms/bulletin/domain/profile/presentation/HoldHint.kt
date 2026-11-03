package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

internal fun formatHoldHint(holdDurationMillis: Int): String {
    val seconds = (holdDurationMillis / 1000.0).let {
        if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
    }
    return "Press and hold for $seconds seconds"
}

@Composable
internal fun HoldHintMessage(hintText: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .padding(top = BulletinSpacing.s),
        contentAlignment = Alignment.Center
    ) {
        if (hintText != null) {
            Text(
                text = hintText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
