package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Two fields joined by an en dash. Each field receives its placeholder ("Min"/"Max" by default). */
@Composable
internal fun MarketplaceRangeFields(
    title: String,
    subtitle: String? = null,
    minimumLabel: String = "Min",
    maximumLabel: String = "Max",
    error: String? = null,
    minimumField: @Composable (Modifier, String) -> Unit,
    maximumField: @Composable (Modifier, String) -> Unit
) {
    MarketplaceFilterSection(title = title, hint = subtitle) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RANGE_GAP),
            verticalAlignment = Alignment.CenterVertically
        ) {
            minimumField(Modifier.weight(1f), minimumLabel)
            Text("–", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            maximumField(Modifier.weight(1f), maximumLabel)
        }
        error?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

private val RANGE_GAP = 10.dp
