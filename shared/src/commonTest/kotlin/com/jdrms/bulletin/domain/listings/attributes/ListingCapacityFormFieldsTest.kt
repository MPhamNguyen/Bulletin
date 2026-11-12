package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeKeys
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.presentation.ListingAttributeDraftActions
import com.jdrms.bulletin.domain.listings.presentation.ListingForm
import com.jdrms.bulletin.domain.listings.presentation.ListingsUiState
import com.jdrms.bulletin.domain.listings.presentation.listingAttributeFormFields
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ListingCapacityFormFieldsTest {
    private fun enabledFields(device: String = ""): Map<String, Boolean> = listingAttributeFormFields(
        ListingCategory.ELECTRONICS,
        mapOf(ListingAttributeKeys.DEVICE_TYPE to device)
    ).associate { it.definition.key to it.enabled }

    @Test
    fun capacityFieldsStayVisibleButDisabledWithoutACompatibleDevice() {
        listOf("", "monitor", "accessory", "other").forEach { device ->
            assertEquals(false, enabledFields(device)[ListingAttributeKeys.RAM_GB])
            assertEquals(false, enabledFields(device)[ListingAttributeKeys.STORAGE_GB])
        }
    }

    @Test
    fun unavailableFieldsExplainTheDevicePrerequisite() {
        val noDevice = listingAttributeFormFields(ListingCategory.ELECTRONICS, emptyMap())
            .first { it.definition.key == ListingAttributeKeys.RAM_GB }
        assertEquals("Select a device type to choose RAM.", noDevice.unavailableReason)
        val console = listingAttributeFormFields(
            ListingCategory.ELECTRONICS,
            mapOf(ListingAttributeKeys.DEVICE_TYPE to "console")
        )
        assertEquals(
            "RAM isn't available for game console.",
            console.first { it.definition.key == ListingAttributeKeys.RAM_GB }.unavailableReason
        )
        assertEquals(null, console.first { it.definition.key == ListingAttributeKeys.STORAGE_GB }.unavailableReason)
    }

    @Test
    fun compatibleDevicesEnableTheSupportedCapacities() {
        listOf("laptop", "desktop", "tablet", "phone").forEach { device ->
            assertEquals(true, enabledFields(device)[ListingAttributeKeys.RAM_GB])
            assertEquals(true, enabledFields(device)[ListingAttributeKeys.STORAGE_GB])
        }
        assertEquals(false, enabledFields("console")[ListingAttributeKeys.RAM_GB])
        assertEquals(true, enabledFields("console")[ListingAttributeKeys.STORAGE_GB])
    }

    @Test
    fun changingToAnIncompatibleDeviceClearsCapacitySelectionsForBothForms() {
        ListingForm.entries.forEach { form ->
            val state = MutableStateFlow(
                ListingsUiState(newCategory = ListingCategory.ELECTRONICS, editCategory = ListingCategory.ELECTRONICS)
            )
            val actions = ListingAttributeDraftActions(state)
            actions.inputChanged(form, "device_type", "laptop")
            actions.inputChanged(form, "ram_gb", "16")
            actions.inputChanged(form, "storage_gb", "512")
            actions.inputChanged(form, "device_type", "console")
            var draft = if (form == ListingForm.NEW) state.value.newAttributes else state.value.editAttributes
            assertFalse("ram_gb" in draft.inputs)
            assertEquals("512", draft.inputs["storage_gb"])
            actions.inputChanged(form, "device_type", "")
            draft = if (form == ListingForm.NEW) state.value.newAttributes else state.value.editAttributes
            assertFalse("ram_gb" in draft.inputs)
            assertFalse("storage_gb" in draft.inputs)
        }
    }
}
