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
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeKind
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeSpec
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory

@Composable
internal fun MarketplaceAttributeFilterFields(
    category: MarketplaceCategory,
    draft: MarketplaceAttributeFilterDraft,
    errors: Map<String, String>,
    onChange: (MarketplaceAttributeFilterDraft) -> Unit
) {
    val selectedChoices = draft.inputs.mapNotNull { (key, input) ->
        input.exact.takeIf(String::isNotBlank)?.let { key to it }
    }.toMap()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MarketplaceAttributeCatalog.applicableSpecs(category, selectedChoices).forEach { spec ->
            val input = draft.input(spec.key)
            when (spec.kind) {
                MarketplaceAttributeKind.CHOICE -> ChoiceField(
                    spec,
                    spec.choicesFor(selectedChoices),
                    input.exact
                ) {
                    onChange(draft.withExact(spec.key, it).reconciled(category))
                }
                MarketplaceAttributeKind.TEXT -> FilterTextField(
                    label = "${spec.label} (exact match)",
                    value = input.exact,
                    error = errors[spec.key],
                    keyboardType = KeyboardType.Text,
                    onValueChange = { onChange(draft.withExact(spec.key, it)) }
                )
                MarketplaceAttributeKind.INTEGER,
                MarketplaceAttributeKind.DECIMAL,
                MarketplaceAttributeKind.DATE -> RangeField(spec, input, errors[spec.key], draft, onChange)
            }
        }
    }
}

@Composable
private fun ChoiceField(
    spec: MarketplaceAttributeSpec,
    choices: Map<String, String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(spec.label, style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            choices.forEach { (code, label) ->
                FilterChip(
                    selected = selected == code,
                    onClick = { onSelect(if (selected == code) "" else code) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
private fun RangeField(
    spec: MarketplaceAttributeSpec,
    input: MarketplaceAttributeFilterInput,
    error: String?,
    draft: MarketplaceAttributeFilterDraft,
    onChange: (MarketplaceAttributeFilterDraft) -> Unit
) {
    val isDate = spec.kind == MarketplaceAttributeKind.DATE
    val unit = spec.suffix.takeIf(String::isNotBlank).orEmpty()
    val label = "${spec.label}${if (unit.isEmpty()) "" else " ($unit)"}"
    val keyboard = when (spec.kind) {
        MarketplaceAttributeKind.INTEGER -> KeyboardType.Number
        MarketplaceAttributeKind.DECIMAL -> KeyboardType.Decimal
        else -> KeyboardType.Text
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterTextField(
                "${if (isDate) "Earliest" else "Minimum"} $label",
                input.min,
                error?.let { "" },
                keyboard,
                Modifier.weight(1f)
            ) { onChange(draft.withMin(spec.key, it)) }
            FilterTextField(
                "${if (isDate) "Latest" else "Maximum"} $label",
                input.max,
                error?.let { "" },
                keyboard,
                Modifier.weight(1f)
            ) { onChange(draft.withMax(spec.key, it)) }
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
        options = BulletinFieldOptions(keyboardOptions = KeyboardOptions(keyboardType = keyboardType))
    )
}
