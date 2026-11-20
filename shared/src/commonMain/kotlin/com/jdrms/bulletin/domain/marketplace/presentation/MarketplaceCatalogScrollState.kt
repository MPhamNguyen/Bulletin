package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
internal fun rememberMarketplaceCatalogScrollState(scrollToTopRequest: Int): LazyGridState {
    val gridState = rememberLazyGridState()
    var handledRequest by rememberSaveable { mutableIntStateOf(scrollToTopRequest) }
    LaunchedEffect(scrollToTopRequest) {
        if (handledRequest != scrollToTopRequest) {
            handledRequest = scrollToTopRequest
            gridState.animateScrollToItem(0)
        }
    }
    return gridState
}
