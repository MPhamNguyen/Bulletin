package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.BulletinFieldDecoration
import com.jdrms.bulletin.core.designsystem.BulletinFieldOptions
import com.jdrms.bulletin.core.designsystem.BulletinTextField
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

private const val MAX_COMMENT_LENGTH = 300

@Composable
internal fun BuyerRatingComment(comment: String, onCommentChange: (String) -> Unit) {
    BulletinTextField(
        value = comment,
        onValueChange = onCommentChange,
        modifier = Modifier.padding(top = BulletinSpacing.m).fillMaxWidth(),
        decoration = BulletinFieldDecoration(
            label = { Text("Comment (optional)") },
            placeholder = { Text("Share details of your experience (optional)...") },
            supportingText = { Text("${comment.length} / $MAX_COMMENT_LENGTH") }
        ),
        options = BulletinFieldOptions(minLines = 3, maxLines = 5)
    )
}
