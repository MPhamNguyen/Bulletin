package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonDefaults

internal data class CreateListingFooterState(
    val step: CreateListingStep,
    val canContinue: Boolean,
    val isSubmitting: Boolean
)

@Composable
internal fun CreateListingFooter(
    state: CreateListingFooterState,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!state.step.isFirst) {
                OutlinedButton(
                    onClick = onBack,
                    enabled = !state.isSubmitting,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    Text("Back", fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = onContinue,
                enabled = state.canContinue && !state.isSubmitting,
                shape = MaterialTheme.shapes.medium,
                colors = BulletinButtonDefaults.buttonColors(),
                modifier = Modifier.weight(1f).heightIn(min = 52.dp)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (state.step.isLast) "Post Listing" else "Continue", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
