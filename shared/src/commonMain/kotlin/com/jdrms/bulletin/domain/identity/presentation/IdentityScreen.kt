package com.jdrms.bulletin.domain.identity.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.SectionHeader

@Composable
fun IdentityScreen(viewModel: IdentityViewModel) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Account & Identity",
                subtitle = "Manage student profile, email verification, and authentication"
            )
        }

        if (state.errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = state.errorMessage!!,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        val session = state.currentSession
        if (session != null && session.isLoggedIn) {
            item {
                BulletinCard {
                    Text(
                        "Logged in as ${state.profile?.fullName ?: session.userId.value}",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("University: ${state.profile?.university ?: "CSU Long Beach"}")
                    Text("Email: ${state.profile?.email?.value ?: ""}")
                    Text("Verified: ${if (state.profile?.isVerified == true) "Yes (Student Verified)" else "No"}")
                    Spacer(Modifier.height(8.dp))
                    Text("Bio: ${state.profile?.bio ?: "No bio yet."}")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.logout() }) {
                        Text("Log Out")
                    }
                }
            }

            item {
                SectionHeader(title = "Student Directory Search")
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    label = { Text("Search by name or email") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            items(state.searchResults) { foundProfile ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(foundProfile.fullName, fontWeight = FontWeight.Bold)
                        Text(foundProfile.university, style = MaterialTheme.typography.bodySmall)
                        Text(foundProfile.email.value, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            item {
                BulletinCard {
                    Text(
                        "Student Authentication",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.emailInput,
                        onValueChange = { viewModel.onEmailChanged(it) },
                        label = { Text("School Email (.edu)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.passwordInput,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.login() }) {
                            Text("Log In")
                        }
                    }
                }
            }

            item {
                BulletinCard {
                    Text(
                        "Verify University Email",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.verificationCodeInput,
                        onValueChange = { viewModel.onVerificationCodeChanged(it) },
                        label = { Text("Verification Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.verifyEmail() }) {
                        Text("Verify Code")
                    }
                }
            }
        }
    }
}
