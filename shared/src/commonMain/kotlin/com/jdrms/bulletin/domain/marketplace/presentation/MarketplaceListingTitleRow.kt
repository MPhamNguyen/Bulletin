package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.skeleton

/** The detail header's title and price. Both are the landing spots of the card's travelling texts. */
@Composable
internal fun MarketplaceListingTitleRow(title: String?, price: String?, isLoading: Boolean) {
    val targets = LocalListingTransitionTargets.current
    val titleProbe = remember { ListingTextProbe() }
    val priceProbe = remember { ListingTextProbe() }
    val style = MaterialTheme.typography.headlineSmall
    val titleColor = MaterialTheme.colorScheme.onSurface
    val priceColor = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = title ?: "Listing title",
            style = style,
            color = titleColor,
            onTextLayout = titleProbe::laidOut,
            modifier = Modifier.weight(1f)
                .skeleton(isLoading && title == null)
                .listingTransitionDestination(ListingTransitionElement.TITLE, targets, titleProbe, style, titleColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = price ?: "$00.00",
            style = style,
            color = priceColor,
            onTextLayout = priceProbe::laidOut,
            modifier = Modifier
                .skeleton(isLoading && price == null, color = priceColor)
                .listingTransitionDestination(ListingTransitionElement.PRICE, targets, priceProbe, style, priceColor)
        )
    }
}
