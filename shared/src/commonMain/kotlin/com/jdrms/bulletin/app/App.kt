package com.jdrms.bulletin.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.core.common.Greeting
import com.jdrms.bulletin.core.designsystem.BulletinTheme
import com.jdrms.bulletin.domain.create_listings.presentation.ReputationScreen
import com.jdrms.bulletin.domain.home.presentation.RecommendationsScreen
import com.jdrms.bulletin.domain.inbox.presentation.MessagingScreen
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceScreen
import com.jdrms.bulletin.domain.profile.presentation.IdentityScreen

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
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
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
                BulletinBottomNavigationBar(
                    currentDestination = currentDestination,
                    onDestinationSelected = { currentDestination = it }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                when (currentDestination) {
                    AppDestination.HOME -> RecommendationsScreen(recommendationsViewModel)
                    AppDestination.MARKETPLACE -> MarketplaceScreen(marketplaceViewModel)
                    AppDestination.CREATE_LISTING -> ReputationScreen(reputationViewModel)
                    AppDestination.MESSAGING -> MessagingScreen(messagingViewModel)
                    AppDestination.PROFILE -> IdentityScreen(identityViewModel)
                }
            }
        }
    }
}

@Composable
fun BulletinBottomNavigationBar(
    currentDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                    .height(68.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppDestination.entries.forEach { destination ->
                    if (destination == AppDestination.CREATE_LISTING) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                onClick = { onDestinationSelected(destination) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                shadowElevation = if (currentDestination == destination) 4.dp else 2.dp,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = destination.label,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        val isSelected = currentDestination == destination
                        val contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 24.dp)
                                ) {
                                    onDestinationSelected(destination)
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = destination.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}
