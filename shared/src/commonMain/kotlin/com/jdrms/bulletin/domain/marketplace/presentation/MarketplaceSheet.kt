package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes

/** Shared shell for the filter and sort sheets: page-colored, no drag handle, dimmed scrim. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MarketplaceBottomSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    tall: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        modifier = if (tall) Modifier.fillMaxHeight() else Modifier,
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        shape = BulletinShapes.sheetLarge,
        containerColor = MaterialTheme.colorScheme.background,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA),
        dragHandle = null,
        content = content
    )
}

@Composable
internal fun MarketplaceSheetHeader(
    title: String,
    onClose: () -> Unit,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 12.dp, top = 18.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
        IconButton(onClick = onClose) {
            Icon(Icons.Outlined.Close, contentDescription = "Close $title", modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
internal fun MarketplaceSheetFooter(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 20.dp),
            content = content
        )
    }
}

private const val SCRIM_ALPHA = 0.55f
