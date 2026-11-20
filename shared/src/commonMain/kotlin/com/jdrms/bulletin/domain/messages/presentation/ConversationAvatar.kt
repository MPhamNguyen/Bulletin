package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes

@Composable
internal fun ConversationAvatar(name: String, modifier: Modifier = Modifier) {
    val initials = name.trim()
        .split(Regex("\\s+"))
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
    Surface(
        modifier = modifier.size(40.dp).semantics { contentDescription = "$name avatar" },
        shape = BulletinShapes.pill,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(initials.ifBlank { "?" }, style = MaterialTheme.typography.labelLarge)
        }
    }
}
