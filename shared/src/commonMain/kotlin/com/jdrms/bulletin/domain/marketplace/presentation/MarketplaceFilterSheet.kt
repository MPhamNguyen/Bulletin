package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinButtonSize
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import kotlinx.coroutines.launch

/** Full-height filter sheet: pinned header and footer around a scrolling body. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MarketplaceFilterSheet(
    filters: MarketplaceFilterState,
    ownsCategoryAndSort: Boolean,
    onApply: (MarketplaceFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    val state = remember(filters, ownsCategoryAndSort) { MarketplaceFilterSheetState(filters, ownsCategoryAndSort) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    MarketplaceBottomSheet(onDismiss = onDismiss, sheetState = sheetState, tall = true) {
        Column(modifier = Modifier.fillMaxSize()) {
            MarketplaceSheetHeader(
                title = "Filters",
                onClose = onDismiss,
                action = {
                    BulletinButton(
                        onClick = state::reset,
                        enabled = state.hasActiveFilters,
                        variant = BulletinButtonVariant.Text
                    ) { Text("Reset all") }
                }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 20.dp)
            ) {
                MarketplaceFilterSheetBody(state)
            }
            MarketplaceSheetFooter {
                BulletinButton(
                    onClick = {
                        state.apply()?.let { applied ->
                            coroutineScope.launch {
                                sheetState.hide()
                                onApply(applied)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    size = BulletinButtonSize.Large
                ) { Text("Show results") }
            }
        }
    }
}
