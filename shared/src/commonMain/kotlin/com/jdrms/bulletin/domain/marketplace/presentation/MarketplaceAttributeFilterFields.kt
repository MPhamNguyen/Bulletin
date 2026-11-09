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
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilterMode
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
            when {
                spec.kind == MarketplaceAttributeKind.CHOICE -> ChoiceField(
                    spec,
                    spec.choicesFor(selectedChoices),
                    input.exact
                ) {
                    onChange(draft.withExact(spec.key, it).reconciled(category))
                }
                spec.kind == MarketplaceAttributeKind.TEXT -> MarketplaceExactFilterField(
                    title = spec.label,
                    subtitle = "Exact match",
                    value = input.exact,
                    error = errors[spec.key],
                    onValueChange = { onChange(draft.withExact(spec.key, it)) }
                )
                spec.kind == MarketplaceAttributeKind.INTEGER &&
                    spec.filterMode == MarketplaceAttributeFilterMode.EXACT -> MarketplaceExactFilterField(
                    title = spec.label,
                    subtitle = if (spec.key == MarketplaceAttributeCatalog.EDITION) {
                        "Specific edition number"
                    } else {
                        "Specific capacity"
                    },
                    value = input.exact,
                    error = errors[spec.key],
                    keyboardType = KeyboardType.Number,
                    suffix = spec.suffix,
                    onValueChange = { onChange(draft.withExact(spec.key, it)) }
                )
                else -> RangeField(spec, input, errors[spec.key], draft, onChange)
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
    val keyboard = when (spec.kind) {
        MarketplaceAttributeKind.INTEGER -> KeyboardType.Number
        MarketplaceAttributeKind.DECIMAL -> KeyboardType.Decimal
        else -> KeyboardType.Text
    }
    MarketplaceRangeFields(
        title = spec.label,
        subtitle = if (isDate) "Use YYYY-MM-DD" else null,
        minimumLabel = if (isDate) "Earliest" else "Minimum",
        maximumLabel = if (isDate) "Latest" else "Maximum",
        error = error,
        minimumField = { modifier ->
            FilterTextField(
                value = input.min,
                error = error?.let { "" },
                keyboardType = keyboard,
                modifier = modifier,
                decoration = rangeFieldDecoration(spec)
            ) { onChange(draft.withMin(spec.key, it)) }
        },
        maximumField = { modifier ->
            FilterTextField(
                value = input.max,
                error = error?.let { "" },
                keyboardType = keyboard,
                modifier = modifier,
                decoration = rangeFieldDecoration(spec)
            ) { onChange(draft.withMax(spec.key, it)) }
        }
    )
}

@Composable
private fun FilterTextField(
    value: String,
    error: String?,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier.fillMaxWidth(),
    decoration: BulletinFieldDecoration = BulletinFieldDecoration(),
    onValueChange: (String) -> Unit
) {
    BulletinTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        isError = error != null,
        decoration = BulletinFieldDecoration(
            label = decoration.label,
            placeholder = decoration.placeholder,
            leadingIcon = decoration.leadingIcon,
            trailingIcon = decoration.trailingIcon,
            supportingText = error?.takeIf(String::isNotEmpty)?.let { message -> { Text(message) } }
        ),
        options = BulletinFieldOptions(keyboardOptions = KeyboardOptions(keyboardType = keyboardType))
    )
}

private fun rangeFieldDecoration(spec: MarketplaceAttributeSpec) = BulletinFieldDecoration(
    leadingIcon = spec.prefix.takeIf(String::isNotBlank)?.let { text -> { Text(text.trim()) } },
    trailingIcon = spec.suffix.takeIf(String::isNotBlank)?.let { text -> { Text(text.trim()) } }
)
