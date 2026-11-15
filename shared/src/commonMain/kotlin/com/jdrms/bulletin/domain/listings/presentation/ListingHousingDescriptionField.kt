package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.jdrms.bulletin.core.designsystem.BulletinFieldDecoration
import com.jdrms.bulletin.core.designsystem.BulletinFieldOptions
import com.jdrms.bulletin.core.designsystem.BulletinTextField
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun ListingHousingDescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
        ListingRequiredLabel("Housing description", style = MaterialTheme.typography.titleSmall)
        BulletinTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Housing description" },
            decoration = BulletinFieldDecoration(
                placeholder = {
                    Text("Describe the space, amenities, utilities, lease terms, and any additional costs.")
                }
            ),
            options = BulletinFieldOptions(minLines = 3, maxLines = 6)
        )
    }
}
