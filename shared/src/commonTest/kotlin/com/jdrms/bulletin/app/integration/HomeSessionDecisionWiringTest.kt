package com.jdrms.bulletin.app.integration

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.app.di.createHomeViewModel
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SwipeVerdict
import com.jdrms.bulletin.domain.home.presentation.HomeDeckExclusion
import com.jdrms.bulletin.domain.home.presentation.HomeViewModel
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeSessionDecisionWiringTest {
    @Test
    fun saveSurvivesViewModelRecreationButANewContainerStartsEmpty() = sessionDecisionTest { f ->
        val first = f.createViewModel()
        advanceUntilIdle()
        val saved = first.uiState.value.feed.first().id
        first.excludeLoadedCard(saved, HomeDeckExclusion.SAVE)
        assertFalse(first.uiState.value.feed.any { it.id == saved })
        f.clearViewModels()
        val recreated = f.createViewModel()
        advanceUntilIdle()
        assertTrue(recreated.uiState.value.loadedItems.any { it.id == saved })
        assertFalse(recreated.uiState.value.feed.any { it.id == saved })
        assertEquals(setOf(saved), f.excluded("first", SwipeVerdict.SAVE))
        val fresh = AppContainer(isInspectionMode = true)
        assertEquals(emptySet(), fresh.swipeDecisionRepository.getExcludedListingIds("first", SwipeVerdict.SAVE).getOrThrow())
    }

    @Test
    fun accountSwitchAndSignOutClearTheRepositoryIncludingTheTransitionToNull() = sessionDecisionTest { f ->
        val viewModel = f.createViewModel()
        advanceUntilIdle()
        val listing = viewModel.uiState.value.feed.first().id
        viewModel.excludeLoadedCard(listing, HomeDeckExclusion.PASS)
        assertEquals(setOf(listing), f.excluded("first", SwipeVerdict.PASS))
        f.signIn("second")
        advanceUntilIdle()
        assertEquals(emptySet(), f.excluded("first", SwipeVerdict.PASS))
        viewModel.excludeLoadedCard(listing, HomeDeckExclusion.SAVE)
        assertEquals(setOf(listing), f.excluded("second", SwipeVerdict.SAVE))
        f.container.sessionRepository.unauthenticated()
        advanceUntilIdle()
        assertEquals(emptySet(), f.excluded("second", SwipeVerdict.SAVE))
        f.signIn("second")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.feed.any { it.id == listing })
    }

    @Test
    fun replayClearsOnlyPassInTheSharedRepository() = sessionDecisionTest { f ->
        val viewModel = f.createViewModel()
        advanceUntilIdle()
        val ids = viewModel.uiState.value.feed.take(2).map { it.id }
        viewModel.excludeLoadedCard(ids[0], HomeDeckExclusion.PASS)
        viewModel.excludeLoadedCard(ids[1], HomeDeckExclusion.SAVE)
        viewModel.replayLoadedCards()
        assertEquals(emptySet(), f.excluded("first", SwipeVerdict.PASS))
        assertEquals(setOf(ids[1]), f.excluded("first", SwipeVerdict.SAVE))
        assertTrue(viewModel.uiState.value.feed.any { it.id == ids[0] })
        assertFalse(viewModel.uiState.value.feed.any { it.id == ids[1] })
    }
}

private class HomeSessionFixture {
    val container = AppContainer(isInspectionMode = true)
    private val stores = mutableListOf<ViewModelStore>()

    init { signIn("first") }

    fun signIn(id: String) {
        container.sessionRepository.authenticated(StudentProfile(UserId(id), StudentEmail("$id@university.edu"), id))
    }

    fun createViewModel(): HomeViewModel {
        val store = ViewModelStore().also(stores::add)
        return ViewModelProvider.create(
            store,
            viewModelFactory { initializer { container.createHomeViewModel() } }
        )[HomeViewModel::class]
    }

    suspend fun excluded(userId: String, verdict: SwipeVerdict): Set<HomeItemId> =
        container.swipeDecisionRepository.getExcludedListingIds(userId, verdict).getOrThrow()

    fun clearViewModels() {
        stores.forEach { it.clear() }
        stores.clear()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun sessionDecisionTest(block: suspend TestScope.(HomeSessionFixture) -> Unit) = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val fixture = HomeSessionFixture()
    try {
        block(fixture)
    } finally {
        fixture.clearViewModels()
        Dispatchers.resetMain()
    }
}
