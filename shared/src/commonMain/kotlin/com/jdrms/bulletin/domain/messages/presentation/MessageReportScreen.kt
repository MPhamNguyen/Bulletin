package com.jdrms.bulletin.domain.messages.presentation

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinExtras
import com.jdrms.bulletin.core.designsystem.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults
import com.jdrms.bulletin.domain.reporting.domain.model.ReportReason
import com.jdrms.bulletin.domain.reporting.domain.model.ReportType

internal data class MessageReportCallbacks(
    val onReasonSelected: (ReportReason) -> Unit,
    val onDescriptionChanged: (String) -> Unit,
    val onSubmit: () -> Unit,
    val onDismiss: () -> Unit
)

@Composable
internal fun MessageReportScreen(
    form: MessageReportFormState,
    isSubmitting: Boolean,
    isInboxLoading: Boolean,
    callbacks: MessageReportCallbacks,
    errorMessage: String?
) {
    Dialog(
        onDismissRequest = { if (!isSubmitting) callbacks.onDismiss() },
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
                Text("Report message", style = MaterialTheme.typography.titleLarge)
                if (form.isSubmitted) {
                    MessageReportSubmitted(callbacks.onDismiss)
                } else {
                    errorMessage?.let { message -> MessageReportError(message) }
                    MessageReportForm(form, callbacks.onReasonSelected, callbacks.onDescriptionChanged)
                    MessageReportSubmitButton(isSubmitting, isInboxLoading, callbacks.onSubmit)
                }
            }
        }
    }
}

@Composable
private fun MessageReportError(message: String) {
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
private fun MessageReportSubmitted(onDismiss: () -> Unit) {
    Surface(
        color = BulletinExtras.colors.successContainer,
        contentColor = BulletinExtras.colors.onSuccessContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Column(
            Modifier.fillMaxWidth().padding(BulletinSpacing.m),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)
        ) {
            Text("Report submitted", style = MaterialTheme.typography.titleMedium)
            Text(
                "Your report was submitted successfully. Thank you for helping keep messaging safe.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
}

@Composable
private fun MessageReportForm(
    form: MessageReportFormState,
    onReasonSelected: (ReportReason) -> Unit,
    onDescriptionChanged: (String) -> Unit
) {
    Column(
        Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Why are you reporting this message?", style = MaterialTheme.typography.titleMedium)
        ReportReason.forType(ReportType.MESSAGE).forEach { reason ->
            MessageReportReasonOption(reason, form.selectedReason, onReasonSelected)
        }
        OutlinedTextField(
            value = form.description,
            onValueChange = onDescriptionChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Additional information (optional)") },
            supportingText = { Text("${form.description.length}/1000") },
            minLines = 3,
            maxLines = 6,
            colors = BulletinTextFieldDefaults.colors()
        )
    }
}

@Composable
private fun MessageReportReasonOption(
    reason: ReportReason,
    selectedReason: ReportReason?,
    onReasonSelected: (ReportReason) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable { onReasonSelected(reason) }.padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selectedReason == reason, onClick = { onReasonSelected(reason) })
        Text(reason.label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MessageReportSubmitButton(
    isSubmitting: Boolean,
    isInboxLoading: Boolean,
    onSubmit: () -> Unit
) {
    Button(
        onClick = onSubmit,
        enabled = !isSubmitting && !isInboxLoading,
        modifier = Modifier.fillMaxWidth(),
        colors = BulletinButtonDefaults.buttonColors()
    ) {
        if (isSubmitting || isInboxLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(if (isSubmitting) "Submitting report…" else "Loading conversations…")
            }
        } else {
            Text("Submit report")
        }
    }
}
