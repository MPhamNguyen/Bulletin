package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle

/** Whether a card's matched text is hidden while its travelling copy is drawn. Layout is never affected. */
internal class ListingSourceVisibility {
    var isHidden by mutableStateOf(false)
}

/** Reports a card text's placement to [probe] and hides only its drawing while [visibility] says so. */
internal fun Modifier.listingTransitionSource(
    probe: ListingTextProbe,
    visibility: ListingSourceVisibility,
    style: TextStyle,
    color: Color
): Modifier = this
    .onGloballyPositioned { probe.placed(it, style, color) }
    .drawWithContent { if (!visibility.isHidden) drawContent() }
