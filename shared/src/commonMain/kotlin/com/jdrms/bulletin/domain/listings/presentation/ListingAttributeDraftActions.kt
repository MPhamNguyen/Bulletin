package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeCatalog
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeCategoryTransition
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Attribute-form flow for both the create and edit forms: field input, category switching with a discard
 * warning, and surfacing inline errors. Category and visible fields always change in one state update.
 */
internal class ListingAttributeDraftActions(private val state: MutableStateFlow<ListingsUiState>) {
    private val parser = ListingAttributeInputParser()

    fun inputChanged(form: ListingForm, key: String, text: String) = state.update { current ->
        val draft = current.attributesOf(form)
        val changedInputs = draft.inputs + (key to text)
        val category = current.categoryOf(form)
        val definitions = ListingAttributeCatalog.definitionsFor(category)
        val controllerKeys = definitions.flatMap { definition ->
            listOfNotNull(definition.applicability?.controllingKey) +
                definition.optionGroups.flatMap { it.requiredChoices.keys }
        }.toSet()
        val visibleDefinitions = parser.applicableDefinitions(category, changedInputs).associateBy { it.key }
        val inputs = if (key in controllerKeys) {
            changedInputs.filter { (inputKey, value) ->
                val definition = visibleDefinitions[inputKey] ?: return@filter false
                definition.optionGroups.isEmpty() || value.isBlank() ||
                    definition.choiceOptions(changedInputs).any { it.code == value }
            }
        } else {
            changedInputs
        }
        current.withAttributes(
            form,
            draft.copy(
                inputs = inputs,
                errors = if (key in controllerKeys) {
                    (draft.errors - key - ListingAttributeInputParser.GENERAL_ERROR_KEY)
                        .filterKeys { it in visibleDefinitions }
                } else {
                    draft.errors - key - ListingAttributeInputParser.GENERAL_ERROR_KEY
                }
            )
        ).copy(errorMessage = null, successMessage = null)
    }

    fun selectCategory(form: ListingForm, category: ListingCategory) = state.update { current ->
        val from = current.categoryOf(form)
        if (from == category) return@update current
        val plan = ListingAttributeCategoryTransition.plan(from, category, current.attributesOf(form).inputs)
        if (plan.discardsValues) {
            current.copy(pendingCategoryChange = PendingCategoryChange(form, category, plan.discarded))
        } else {
            current.applyCategory(form, category, plan.retained)
        }
    }

    fun confirmCategoryChange() = state.update { current ->
        val pending = current.pendingCategoryChange ?: return@update current
        val plan = ListingAttributeCategoryTransition.plan(
            current.categoryOf(pending.form),
            pending.target,
            current.attributesOf(pending.form).inputs
        )
        current.applyCategory(pending.form, pending.target, plan.retained)
    }

    /** Leaves the category, inputs, and errors exactly as they were. */
    fun cancelCategoryChange() = state.update { it.copy(pendingCategoryChange = null) }

    fun showErrors(form: ListingForm, errors: Map<String, String>) = state.update { current ->
        current.withAttributes(form, current.attributesOf(form).copy(errors = errors))
    }
}

internal fun ListingsUiState.categoryOf(form: ListingForm): ListingCategory =
    if (form == ListingForm.NEW) newCategory else editCategory

internal fun ListingsUiState.attributesOf(form: ListingForm): ListingAttributeDraft =
    if (form == ListingForm.NEW) newAttributes else editAttributes

private fun ListingsUiState.withAttributes(form: ListingForm, draft: ListingAttributeDraft): ListingsUiState =
    if (form == ListingForm.NEW) copy(newAttributes = draft) else copy(editAttributes = draft)

private fun ListingsUiState.applyCategory(
    form: ListingForm,
    category: ListingCategory,
    retained: Map<String, String>
): ListingsUiState {
    val draft = attributesOf(form).copy(inputs = retained, errors = emptyMap())
    val updated = if (form == ListingForm.NEW) copy(newCategory = category) else copy(editCategory = category)
    return updated.withAttributes(form, draft).copy(pendingCategoryChange = null)
}
