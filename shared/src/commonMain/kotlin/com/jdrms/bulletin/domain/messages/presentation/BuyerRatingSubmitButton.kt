package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun BuyerRatingSubmitButton(isProcessing: Boolean = false, onClick: () -> Unit) {
    BulletinButton(
        onClick = onClick,
        enabled = !isProcessing,
        modifier = Modifier.padding(top = BulletinSpacing.m).fillMaxWidth()
    ) {
        Text(if (isProcessing) "Submitting…" else "Submit Rating", style = MaterialTheme.typography.labelLarge)
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.padding(start = BulletinSpacing.s).size(14.dp)
        )
    }
}
