package com.jdrms.bulletin.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.theme.BulletinExtras
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.theme.BulletinTextFieldDefaults

private const val MAX_COMMENT_LENGTH = 500

@Composable
fun ReviewSubmissionDialog(
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (score: Int, comment: String) -> Unit
) {
    var score by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    val visibility = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isSubmitting,
            dismissOnClickOutside = !isSubmitting
        )
    ) {
        AnimatedVisibility(
            visibleState = visibility,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight / 2 },
                animationSpec = tween(durationMillis = 280)
            ) + fadeIn(animationSpec = tween(durationMillis = 220))
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.92f),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 20.dp
            ) {
                Column(
                    modifier = Modifier.dismissKeyboardOnTapOutside().padding(BulletinSpacing.xl),
                    verticalArrangement = Arrangement.spacedBy(BulletinSpacing.l)
                ) {
                    ReviewHeader()
                    ReviewRatingPicker(score) { score = it }
                    ReviewCommentField(comment) {
                        comment = it.take(MAX_COMMENT_LENGTH)
                    }
                    ReviewError(errorMessage)
                    ReviewActions(score, comment, isSubmitting, onDismiss, onSubmit)
                }
            }
        }
    }
}

@Composable
private fun ReviewHeader() {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = BulletinExtras.colors.star,
                modifier = Modifier.padding(BulletinSpacing.m)
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Share your experience", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "How was your purchase from this seller?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReviewRatingPicker(score: Int, onScoreChange: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
            .padding(vertical = BulletinSpacing.m),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)) {
            for (value in 1..5) {
                IconButton(onClick = { onScoreChange(value) }) {
                    Icon(
                        imageVector = if (value <= score) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "$value star${if (value == 1) "" else "s"}",
                        tint = BulletinExtras.colors.star,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
        Text(
            text = if (score == 0) "Tap a star to rate" else "$score out of 5 stars",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReviewCommentField(comment: String, onCommentChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)) {
        OutlinedTextField(
            value = comment,
            onValueChange = onCommentChange,
            label = { Text("Comment (optional)") },
            placeholder = { Text("Add details if you’d like") },
            colors = BulletinTextFieldDefaults.colors(),
            modifier = Modifier.showKeyboardOnFocusOrTap().fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )
        Text(
            "${comment.length}/$MAX_COMMENT_LENGTH",
            modifier = Modifier.align(Alignment.End),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReviewError(errorMessage: String?) {
    errorMessage?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ReviewActions(
    score: Int,
    comment: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Cancel") }
        Button(
            onClick = { onSubmit(score, comment) },
            enabled = !isSubmitting && score in 1..5,
            colors = BulletinButtonDefaults.buttonColors()
        ) {
            Text(if (isSubmitting) "Submitting…" else "Submit review")
        }
    }
}
