package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReviewSubmissionDialog(
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (score: Int, comment: String) -> Unit
) {
    var score by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = BulletinExtras.colors.star,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Leave Review for Seller", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "How was your experience buying this item?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            for (value in 1..5) {
                                IconButton(
                                    onClick = { score = value },
                                    enabled = !isSubmitting
                                ) {
                                    Icon(
                                        imageVector = if (value <= score) {
                                            Icons.Filled.Star
                                        } else {
                                            Icons.Outlined.StarBorder
                                        },
                                        contentDescription = "$value star${if (value == 1) "" else "s"}",
                                        tint = BulletinExtras.colors.star,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (score == 0) "Select a rating" else "$score out of 5 stars",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it.take(MAX_COMMENT_LENGTH) },
                    label = { Text("Comment") },
                    placeholder = { Text("Share a little about your experience") },
                    colors = BulletinTextFieldDefaults.colors(),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    enabled = !isSubmitting
                )
                Text(
                    text = "${comment.length}/$MAX_COMMENT_LENGTH",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Cancel") }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(score, comment) },
                enabled = !isSubmitting && score in 1..5 && comment.isNotBlank(),
                colors = BulletinButtonDefaults.buttonColors()
            ) { Text(if (isSubmitting) "Submitting..." else "Submit Review") }
        }
    )
}

private const val MAX_COMMENT_LENGTH = 500
