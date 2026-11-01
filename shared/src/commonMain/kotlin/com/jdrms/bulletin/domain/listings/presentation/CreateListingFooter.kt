package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinButtonSize
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant

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
                BulletinButton(
                    onClick = onBack,
                    enabled = !state.isSubmitting,
                    modifier = Modifier,
                    variant = BulletinButtonVariant.Outlined,
                    size = BulletinButtonSize.Large
                ) {
                    Text("Back", fontWeight = FontWeight.Bold)
                }
            }
            BulletinButton(
                onClick = onContinue,
                enabled = state.canContinue && !state.isSubmitting,
                modifier = Modifier.weight(1f),
                variant = BulletinButtonVariant.Primary,
                size = BulletinButtonSize.Large
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
