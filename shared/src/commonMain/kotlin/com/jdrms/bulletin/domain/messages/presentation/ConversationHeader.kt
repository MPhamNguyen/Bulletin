package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.jdrms.bulletin.core.designsystem.BulletinSpacing
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant

internal data class ConversationHeaderActions(
    val onBack: () -> Unit,
    val onViewProfile: (ConversationParticipant) -> Unit,
    val onMoreOptions: () -> Unit
)

@Composable
internal fun ConversationHeader(
    counterpart: ConversationParticipant?,
    fallbackTitle: String,
    isMoreOptionsEnabled: Boolean,
    actions: ConversationHeaderActions,
    modifier: Modifier = Modifier
) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.padding(BulletinSpacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
        ) {
            IconButton(actions.onBack, Modifier.size(BulletinSpacing.control)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to conversations")
            }
            val name = counterpart?.displayName ?: fallbackTitle
            Row(
                Modifier.weight(1f).heightIn(min = BulletinSpacing.control)
                    .clickable(enabled = counterpart != null, role = Role.Button, onClickLabel = "View profile") {
                        counterpart?.let(actions.onViewProfile)
                    }.padding(BulletinSpacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
            ) {
                ConversationAvatar(name)
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(actions.onMoreOptions, Modifier.size(BulletinSpacing.control), isMoreOptionsEnabled) {
                Icon(Icons.Outlined.MoreVert, "More options")
            }
        }
    }
}
