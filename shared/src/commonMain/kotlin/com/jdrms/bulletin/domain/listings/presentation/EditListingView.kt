package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.common.ImageCropSettings
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinButtonSize
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinFieldDecoration
import com.jdrms.bulletin.core.designsystem.BulletinFieldOptions
import com.jdrms.bulletin.core.designsystem.BulletinTextField
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition

data class EditListingActions(
    val onTitleChange: (String) -> Unit,
    val onPriceChange: (String) -> Unit,
    val onCategorySelected: (ListingCategory) -> Unit,
    val onConditionSelected: (ListingCondition) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onImagesSelected: (List<com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload>) -> Unit,
    val onImageSelectionError: (String) -> Unit,
    val onRemoveImage: (Int) -> Unit,
    val onCropImage: (Int, ImageCropSettings) -> Unit,
    val onAttributeChange: (String, String) -> Unit,
    val onConfirmCategoryChange: () -> Unit,
    val onCancelCategoryChange: () -> Unit,
    val onSave: () -> Unit,
    val onBack: () -> Unit
)

@Composable
fun EditListingView(
    uiState: ListingsUiState,
    actions: EditListingActions,
    modifier: Modifier = Modifier
) {
    val pickListingImages = rememberListingImagePicker(actions.onImagesSelected, actions.onImageSelectionError)
    Column(modifier = modifier.fillMaxSize()) {
        EditListingTopBar(onBack = actions.onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BulletinCard {
                Text(
                    text = "Listing Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                BulletinTextField(
                    value = uiState.editTitle,
                    onValueChange = actions.onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    decoration = BulletinFieldDecoration(
                        label = { Text("Item Title") }
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                CurrencyTextField(
                    digits = uiState.editPriceCents,
                    onDigitsChange = actions.onPriceChange,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                ListingCategorySelectionRow(
                    selectedCategory = uiState.editCategory,
                    onCategorySelected = actions.onCategorySelected
                )

                ListingAttributeFields(
                    category = uiState.editCategory,
                    draft = uiState.editAttributes,
                    onInputChange = actions.onAttributeChange,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                ListingConditionSelectionRow(
                    selectedCondition = uiState.editCondition,
                    onConditionSelected = actions.onConditionSelected
                )

                Spacer(modifier = Modifier.height(12.dp))

                BulletinTextField(
                    value = uiState.editDescription,
                    onValueChange = actions.onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    decoration = BulletinFieldDecoration(
                        label = { Text("Description & Campus Pick-Up Details") }
                    ),
                    options = BulletinFieldOptions(
                        minLines = 3,
                        maxLines = 6
                    )
                )
            }

            ListingImageSelectionField(
                photos = uiState.editPhotos,
                onPick = pickListingImages,
                onRemove = actions.onRemoveImage,
                onCropChange = actions.onCropImage
            )

            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            BulletinButton(
                onClick = actions.onSave,
                enabled = !uiState.isUpdating,
                modifier = Modifier
                    .fillMaxWidth(),
                variant = BulletinButtonVariant.Primary,
                size = BulletinButtonSize.Large
            ) {
                if (uiState.isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Save Changes",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            BulletinButton(
                onClick = actions.onBack,
                modifier = Modifier
                    .fillMaxWidth(),
                variant = BulletinButtonVariant.Outlined
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    uiState.pendingCategoryChange?.takeIf { it.form == ListingForm.EDIT }?.let { change ->
        ListingCategoryChangeDialog(change, actions.onConfirmCategoryChange, actions.onCancelCategoryChange)
    }
}
