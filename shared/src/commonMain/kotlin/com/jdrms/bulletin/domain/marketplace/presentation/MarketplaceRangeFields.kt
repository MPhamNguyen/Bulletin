package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun MarketplaceRangeFields(
    title: String,
    subtitle: String? = null,
    minimumLabel: String = "Minimum",
    maximumLabel: String = "Maximum",
    error: String? = null,
    minimumField: @Composable (Modifier) -> Unit,
    maximumField: @Composable (Modifier) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(minimumLabel, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(RANGE_SEPARATOR_WIDTH))
            Text(maximumLabel, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            minimumField(Modifier.weight(1f))
            Text(
                text = "-",
                modifier = Modifier.width(RANGE_SEPARATOR_WIDTH),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
            maximumField(Modifier.weight(1f))
        }
        error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

private val RANGE_SEPARATOR_WIDTH = 32.dp
