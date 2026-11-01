package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.profile.application.BlockedUser

@Composable
internal fun BlockedUsersView(
    state: BlockedUsersUiState,
    onUnblock: (String) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        BlockedUsersTopBar(onBack)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
        ) {
            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            state.errorMessage?.let { message ->
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(message, color = MaterialTheme.colorScheme.error)
                        BulletinButton(onClick = onRefresh, variant = BulletinButtonVariant.Text) { Text("Retry") }
                    }
                }
            }
            if (!state.isLoading && state.errorMessage == null && state.users.isEmpty()) {
                item { EmptyBlockedUsersCard() }
            }
            items(state.users, key = { it.id.value }) { user ->
                BlockedUserCard(
                    user = user,
                    isRemoving = state.removingUserId == user.id.value,
                    actionEnabled = state.removingUserId == null,
                    onUnblock = { onUnblock(user.id.value) }
                )
            }
        }
    }
}

@Composable
private fun BlockedUsersTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = BulletinSpacing.l, vertical = BulletinSpacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back to Settings")
        }
        Spacer(Modifier.width(BulletinSpacing.s))
        Text("Blocked Users", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun BlockedUserCard(
    user: BlockedUser,
    isRemoving: Boolean,
    actionEnabled: Boolean,
    onUnblock: () -> Unit
) {
    val profile = user.profile
    BulletinCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatarImage(
                avatarUrl = profile?.avatarUrl,
                fullName = profile?.fullName ?: "Blocked user",
                size = 48.dp,
                borderWidth = 1.dp
            )
            Spacer(Modifier.width(BulletinSpacing.m))
            Column(Modifier.weight(1f)) {
                Text(
                    profile?.fullName?.ifBlank { null } ?: "Blocked user",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Their listings are hidden from you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BulletinButton(
                onClick = onUnblock,
                enabled = actionEnabled,
                variant = BulletinButtonVariant.Outlined
            ) {
                if (isRemoving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Unblock")
                }
            }
        }
    }
}

@Composable
private fun EmptyBlockedUsersCard() {
    BulletinCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = BulletinSpacing.l),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.Block,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                "No blocked users",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Users you block will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
