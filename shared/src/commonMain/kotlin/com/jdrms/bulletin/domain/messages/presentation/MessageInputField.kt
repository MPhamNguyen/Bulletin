package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinSpacing

@Composable
internal fun MessageInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(BulletinSpacing.xl)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface)
    val outline = when {
        isError -> Modifier.border(2.dp, colors.error, shape)
        isFocused -> Modifier.border(2.dp, colors.primary, shape)
        else -> Modifier
    }
    BasicTextField(
        value = value, onValueChange = onValueChange,
        modifier = modifier.semantics { if (isError) error("Message could not be saved") },
        enabled = enabled, maxLines = 4, textStyle = textStyle,
        cursorBrush = SolidColor(colors.primary), interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Box(
                Modifier.fillMaxWidth().heightIn(min = BulletinSpacing.control)
                    .background(colors.surfaceVariant, shape).then(outline)
                    .padding(horizontal = BulletinSpacing.l, vertical = BulletinSpacing.m),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = textStyle,
                        color = colors.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
                innerTextField()
            }
        }
    )
}
