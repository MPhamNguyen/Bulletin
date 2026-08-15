package com.jdrms.bulletin.domain.messaging.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.SectionHeader

@Composable
fun MessagingScreen(viewModel: MessagingViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SectionHeader(
            title = "Real-Time Student Messages",
            subtitle = "Communicate securely with campus buyers and sellers"
        )

        Spacer(Modifier.height(8.dp))

        if (state.conversations.isEmpty()) {
            BulletinCard {
                Text("No active conversations found.")
            }
        } else {
            Row(modifier = Modifier.weight(1f)) {
                // Conversation list column
                LazyColumn(
                    modifier = Modifier.weight(0.4f).padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.conversations) { conv ->
                        val isSelected = conv.id == state.selectedConversationId
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.selectConversation(conv.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(conv.participantNames.joinToString(", "), fontWeight = FontWeight.Bold)
                                Text(
                                    conv.lastMessage?.text ?: "No messages yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Messages view column
                Column(modifier = Modifier.weight(0.6f)) {
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.currentMessages) { msg ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (msg.isReported) {
                                        MaterialTheme.colorScheme.errorContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        msg.senderName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(msg.text, style = MaterialTheme.typography.bodyMedium)
                                    if (msg.isReported) {
                                        Text(
                                            "[Reported]",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    } else {
                                        TextButton(onClick = { viewModel.report(msg.id, "Inappropriate content") }) {
                                            Text("Report", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.messageInput,
                            onValueChange = { viewModel.onMessageInputChanged(it) },
                            placeholder = { Text("Type a message...") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { viewModel.sendCurrentMessage() }) {
                            Text("Send")
                        }
                    }
                }
            }
        }
    }
}
