package com.jdrms.bulletin.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.core.common.Greeting
import com.jdrms.bulletin.core.designsystem.BulletinTheme
import com.jdrms.bulletin.domain.identity.presentation.IdentityScreen
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceScreen
import com.jdrms.bulletin.domain.messaging.presentation.MessagingScreen
import com.jdrms.bulletin.domain.recommendations.presentation.RecommendationsScreen
import com.jdrms.bulletin.domain.reputation.presentation.ReputationScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(appContainer: AppContainer = remember { AppContainer() }) {
    BulletinTheme {
        var currentDestination by remember { mutableStateOf(AppDestination.MARKETPLACE) }

        val identityViewModel = remember { appContainer.createIdentityViewModel() }
        val marketplaceViewModel = remember { appContainer.createMarketplaceViewModel() }
        val messagingViewModel = remember { appContainer.createMessagingViewModel() }
        val reputationViewModel = remember { appContainer.createReputationViewModel() }
        val recommendationsViewModel = remember { appContainer.createRecommendationsViewModel() }

        val greetingText = remember { Greeting().greet() }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("🎓 Bulletin", fontWeight = FontWeight.Bold)
                            Text(
                                greetingText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    AppDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination == destination,
                            onClick = { currentDestination = destination },
                            label = { Text(destination.label) },
                            icon = { Text(destination.icon) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                when (currentDestination) {
                    AppDestination.MARKETPLACE -> MarketplaceScreen(marketplaceViewModel)
                    AppDestination.RECOMMENDATIONS -> RecommendationsScreen(recommendationsViewModel)
                    AppDestination.MESSAGING -> MessagingScreen(messagingViewModel)
                    AppDestination.REPUTATION -> ReputationScreen(reputationViewModel)
                    AppDestination.IDENTITY -> IdentityScreen(identityViewModel)
                }
            }
        }
    }
}
