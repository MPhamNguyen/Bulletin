package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinFieldDecoration
import com.jdrms.bulletin.core.designsystem.BulletinFieldOptions
import com.jdrms.bulletin.core.designsystem.BulletinTextField
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeCatalog

/** Electronics-only filter inputs inside the filter sheet: device type, exact brand, RAM and storage ranges. */
@Composable
internal fun MarketplaceAttributeFilterFields(
    draft: MarketplaceAttributeFilterDraft,
    errors: Map<String, String>,
    onChange: (MarketplaceAttributeFilterDraft) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Device type", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MarketplaceAttributeCatalog.deviceTypes.forEach { (code, label) ->
                FilterChip(
                    selected = draft.deviceType == code,
                    onClick = { onChange(draft.copy(deviceType = if (draft.deviceType == code) null else code)) },
                    label = { Text(label) }
                )
            }
        }
        FilterTextField(
            "Brand (exact match)",
            draft.brand,
            errors[MarketplaceAttributeFilterDraft.BRAND],
            KeyboardType.Text
        ) {
            onChange(draft.copy(brand = it))
        }
        RangeRow(
            "RAM (GB)",
            draft.ramMin,
            draft.ramMax,
            errors[MarketplaceAttributeFilterDraft.RAM],
            { onChange(draft.copy(ramMin = it)) },
            { onChange(draft.copy(ramMax = it)) }
        )
        RangeRow(
            "Storage (GB)",
            draft.storageMin,
            draft.storageMax,
            errors[MarketplaceAttributeFilterDraft.STORAGE],
            { onChange(draft.copy(storageMin = it)) },
            { onChange(draft.copy(storageMax = it)) }
        )
    }
}

@Composable
private fun RangeRow(
    label: String,
    min: String,
    max: String,
    error: String?,
    onMin: (String) -> Unit,
    onMax: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterTextField("$label min", min, error?.let { "" }, KeyboardType.Number, Modifier.weight(1f), onMin)
            FilterTextField("$label max", max, error?.let { "" }, KeyboardType.Number, Modifier.weight(1f), onMax)
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun FilterTextField(
    label: String,
    value: String,
    error: String?,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onValueChange: (String) -> Unit
) {
    BulletinTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        isError = error != null,
        decoration = BulletinFieldDecoration(
            label = { Text(label) },
            supportingText = error?.takeIf(String::isNotEmpty)?.let { message -> { Text(message) } }
        ),
        options = BulletinFieldOptions(

            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
    )
}
