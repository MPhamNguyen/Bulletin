package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/** Shared measured layout for the pending and interactive message form. */
@Composable
internal fun MessageSellerQuickActionContent(
    status: MessageSellerQuickActionStatus,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isSending: Boolean = false,
    errorMessage: String? = null
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
    ) {
        Text("Message Seller", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BulletinTextField(
                value = status.fieldText,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).skeleton(isLoading),
                decoration = BulletinFieldDecoration(
                    placeholder = { Text(DEFAULT_SELLER_MESSAGE) },
                    leadingIcon = if (status.isMessageSent) {
                        { androidx.compose.material3.Icon(Icons.Filled.Check, contentDescription = "Message sent") }
                    } else {
                        null
                    }
                ),
                options = BulletinFieldOptions(
                    enabled = !isLoading,
                    readOnly = !status.isEditable,
                    minLines = 1,
                    maxLines = 3,
                    textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                        color = if (!status.isEditable) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                )
            )
            BulletinButton(
                onClick = onSend,
                modifier = Modifier.skeleton(isLoading),
                enabled = !isLoading && !isSending &&
                    (status.isMessageSent || (status.isEditable && status.fieldText.isNotBlank())),
                variant = BulletinButtonVariant.Primary
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier, strokeWidth = 2.dp)
                } else {
                    Text(
                        status.buttonText
                    )
                }
            }
        }
        errorMessage?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
