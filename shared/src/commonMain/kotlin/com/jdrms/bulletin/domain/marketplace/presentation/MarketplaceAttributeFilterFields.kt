package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinColorSwatch
import com.jdrms.bulletin.core.designsystem.BulletinFieldDecoration
import com.jdrms.bulletin.core.designsystem.BulletinFieldOptions
import com.jdrms.bulletin.core.designsystem.BulletinTextField
import com.jdrms.bulletin.core.designsystem.theme.BulletinFieldStyle
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeCatalog
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilterMode
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeKind
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeSpec
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityOptions
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
    Column(verticalArrangement = Arrangement.spacedBy(MARKETPLACE_FILTER_SECTION_GAP)) {
        MarketplaceAttributeCatalog.applicableSpecs(category, selectedChoices).forEach { spec ->
            val input = draft.input(spec.key)
            when {
                MarketplaceCapacityOptions.forKey(spec.key).isNotEmpty() -> MarketplaceCapacityFilterField(
                    spec = spec,
                    input = input,
                    onToggle = { onChange(draft.toggleCapacity(spec.key, it)) }
                )
                spec.key == MarketplaceAttributeCatalog.COLOR -> MarketplaceColorFilterField(
                    spec = spec,
                    input = input,
                    onToggle = { onChange(draft.toggleColor(it)) }
                )
                spec.key == MarketplaceAttributeCatalog.SIZE -> MarketplaceSizeFilterField(
                    choices = spec.choicesFor(selectedChoices),
                    input = input,
                    onToggle = { onChange(draft.toggleChoice(spec.key, it)) }
                )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceField(
    spec: MarketplaceAttributeSpec,
    choices: Map<String, String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    MarketplaceFilterSection(title = spec.label) {
        if (spec.key == MarketplaceAttributeCatalog.COLOR) {
            FlowRow {
                choices.forEach { (code, label) ->
                    BulletinColorSwatch(
                        code = code,
                        label = label,
                        selected = selected == code,
                        onClick = { onSelect(if (selected == code) "" else code) }
                    )
                }
            }
        } else {
            MarketplacePillGroup(
                options = choices.keys.toList(),
                isSelected = { it == selected },
                label = { code -> choices.getValue(code) },
                onToggle = { code -> onSelect(if (selected == code) "" else code) }
            )
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
        minimumLabel = if (isDate) "Earliest" else "Min",
        maximumLabel = if (isDate) "Latest" else "Max",
        error = error,
        minimumField = { modifier, placeholder ->
            FilterTextField(
                value = input.min,
                error = error?.let { "" },
                keyboardType = keyboard,
                modifier = modifier,
                decoration = rangeFieldDecoration(spec, placeholder)
            ) { onChange(draft.withMin(spec.key, it)) }
        },
        maximumField = { modifier, placeholder ->
            FilterTextField(
                value = input.max,
                error = error?.let { "" },
                keyboardType = keyboard,
                modifier = modifier,
                decoration = rangeFieldDecoration(spec, placeholder)
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
        options = BulletinFieldOptions(keyboardOptions = KeyboardOptions(keyboardType = keyboardType)),
        style = BulletinFieldStyle.Inset
    )
}

private fun rangeFieldDecoration(spec: MarketplaceAttributeSpec, placeholder: String) = BulletinFieldDecoration(
    placeholder = { Text(placeholder) },
    leadingIcon = spec.prefix.takeIf(String::isNotBlank)?.let { text -> { Text(text.trim()) } },
    trailingIcon = spec.suffix.takeIf(String::isNotBlank)?.let { text -> { Text(text.trim()) } }
)

/** Vertical rhythm between filter sections inside the sheet. */
internal val MARKETPLACE_FILTER_SECTION_GAP = 24.dp
