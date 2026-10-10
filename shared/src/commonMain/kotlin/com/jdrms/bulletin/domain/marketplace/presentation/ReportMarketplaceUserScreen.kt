package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jdrms.bulletin.core.designsystem.BulletinExtras
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceUserReportReason

@Composable
internal fun ReportMarketplaceUserScreen(
    state: MarketplaceUserReportUiState,
    onReasonSelected: (MarketplaceUserReportReason) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!state.isSubmitting) onBack() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 620.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, enabled = !state.isSubmitting) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to seller profile")
                    }
                    Text("Report user", style = MaterialTheme.typography.titleLarge)
                }
                if (state.submitted) {
                    ReportSubmittedContent(onBack)
                } else {
                    state.errorMessage?.let { ReportErrorContent(it) }
                    ReportFormContent(
                        state = state,
                        onReasonSelected = onReasonSelected,
                        onDescriptionChanged = onDescriptionChanged
                    )
                    Button(
                        onClick = onSubmit,
                        enabled = !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isSubmitting) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text("Submitting report…")
                            }
                        } else {
                            Text("Submit report")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportErrorContent(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
    }
}

@Composable
private fun ReportSubmittedContent(onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = BulletinExtras.colors.successContainer,
            contentColor = BulletinExtras.colors.onSuccessContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Report submitted", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Your report was submitted successfully. Thank you for helping keep the marketplace safe.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

@Composable
private fun ReportFormContent(
    state: MarketplaceUserReportUiState,
    onReasonSelected: (MarketplaceUserReportReason) -> Unit,
    onDescriptionChanged: (String) -> Unit
) {
    Column(
        Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Why are you reporting this user?", style = MaterialTheme.typography.titleMedium)
        MarketplaceUserReportReason.entries.forEach { reason ->
            Row(
                Modifier.fillMaxWidth().clickable { onReasonSelected(reason) }.padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(state.selectedReason == reason, onClick = { onReasonSelected(reason) })
                Text(reason.label, style = MaterialTheme.typography.bodyMedium)
            }
        }
        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Additional information (optional)") },
            supportingText = { Text("${state.description.length}/1000") },
            minLines = 3,
            maxLines = 6,
            colors = BulletinTextFieldDefaults.colors()
        )
    }
}
