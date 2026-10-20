package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreference

@Composable
internal fun MarketplaceLayoutToggle(
    selected: MarketplaceLayoutPreference,
    onSelected: (MarketplaceLayoutPreference) -> Unit
) {
    val target = when (selected) {
        MarketplaceLayoutPreference.ONE_PER_ROW -> MarketplaceLayoutPreference.TWO_PER_ROW
        MarketplaceLayoutPreference.TWO_PER_ROW -> MarketplaceLayoutPreference.ONE_PER_ROW
    }
    OutlinedIconButton(
        onClick = { onSelected(target) },
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.size(40.dp)
    ) {
        LayoutColumnsIcon(target.columns, "Switch to ${layoutOptionLabel(target)}")
    }
}

@Composable
private fun LayoutColumnsIcon(columns: Int, description: String? = null) {
    val semanticsModifier = if (description == null) {
        Modifier
    } else {
        Modifier.semantics { contentDescription = description }
    }
    Box(
        modifier = semanticsModifier,
        contentAlignment = Alignment.Center
    ) {
        if (columns == 1) SingleColumnIcon() else TwoColumnIcon()
    }
}

@Composable
private fun SingleColumnIcon() {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(2) { LayoutShape(20.dp, 7.dp) }
    }
}

@Composable
private fun TwoColumnIcon() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(2) { LayoutShape(8.dp, 8.dp) }
            }
        }
    }
}

@Composable
private fun LayoutShape(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .size(width, height)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp))
    )
}

private fun layoutOptionLabel(preference: MarketplaceLayoutPreference): String {
    val listingLabel = if (preference.columns == 1) "listing" else "listings"
    return "${preference.columns} $listingLabel per row"
}
