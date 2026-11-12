package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory

@Composable
fun MarketplaceInterestsScreen(
    viewModel: MarketplaceInterestsViewModel,
    onBack: () -> Unit,
    onListingClick: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var editingCategory by remember { mutableStateOf<MarketplaceCategory?>(null) }
    LaunchedEffect(Unit) {
        viewModel.refreshMatches()
    }
    DisposableEffect(viewModel) {
        viewModel.setInterestsVisible(true)
        onDispose { viewModel.setInterestsVisible(false) }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        MarketplaceInterestsTopBar(onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BulletinSpacing.l, vertical = BulletinSpacing.m),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.l)
        ) {
            BulletinCard {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s),
                        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
                    ) {
                        MarketplaceCategory.entries.forEach { category ->
                            val selected = category in state.selectedCategories
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    viewModel.toggle(category)
                                    editingCategory = if (selected) null else category
                                },
                                label = { Text(category.displayName()) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }
            }
            MarketplaceInterestCategoryFilters(
                interests = state.interests.values.toList(),
                onEdit = { editingCategory = it }
            )
            MarketplaceInterestMatches(
                listings = state.matchingListings,
                isLoading = state.isLoadingMatches,
                onListingClick = onListingClick
            )
            if (state.isSaving) {
                Text(
                    text = "Saving…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    editingCategory?.let { category ->
        state.interests[category]?.let { interest ->
            MarketplaceInterestFilterEditor(
                interest = interest,
                onApply = {
                    viewModel.updateInterest(it)
                    editingCategory = null
                },
                onDismiss = { editingCategory = null }
            )
        }
    }
}

@Composable
private fun MarketplaceInterestsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = BulletinSpacing.l, vertical = BulletinSpacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back to profile"
            )
        }
        Spacer(Modifier.width(BulletinSpacing.s))
        Text("Interests", style = MaterialTheme.typography.titleLarge)
    }
}
