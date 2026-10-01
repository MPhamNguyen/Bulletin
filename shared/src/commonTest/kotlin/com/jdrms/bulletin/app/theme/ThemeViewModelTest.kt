package com.jdrms.bulletin.app.theme

import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsWithLastAppliedPreferenceBeforeAccountIsRestored() = runTest(testDispatcher) {
        val viewModel = ThemeViewModel(
            InMemoryThemePreferenceStore(initialLastAppliedPreference = ThemePreference.DARK)
        )
        advanceUntilIdle()

        assertEquals(ThemePreference.DARK, viewModel.themePreference.value)
    }

    @Test
    fun defaultsToSystemPreference() = runTest(testDispatcher) {
        val viewModel = ThemeViewModel(InMemoryThemePreferenceStore())
        advanceUntilIdle()

        viewModel.setAccount(UserId("user-1"))
        advanceUntilIdle()

        assertEquals(ThemePreference.SYSTEM, viewModel.themePreference.value)
    }

    @Test
    fun settingPreferenceUpdatesState() = runTest(testDispatcher) {
        val viewModel = ThemeViewModel(InMemoryThemePreferenceStore())
        advanceUntilIdle()

        viewModel.setAccount(UserId("user-1"))
        advanceUntilIdle()

        viewModel.setThemePreference(ThemePreference.DARK)
        advanceUntilIdle()

        assertEquals(ThemePreference.DARK, viewModel.themePreference.value)
    }

    @Test
    fun preferenceIsRestoredAcrossViewModelRecreation() = runTest(testDispatcher) {
        val store = InMemoryThemePreferenceStore()
        val firstViewModel = ThemeViewModel(store)
        advanceUntilIdle()

        firstViewModel.setAccount(UserId("user-1"))
        advanceUntilIdle()

        firstViewModel.setThemePreference(ThemePreference.LIGHT)
        advanceUntilIdle()

        val recreatedViewModel = ThemeViewModel(store)
        advanceUntilIdle()

        recreatedViewModel.setAccount(UserId("user-1"))
        advanceUntilIdle()

        assertEquals(ThemePreference.LIGHT, recreatedViewModel.themePreference.value)
    }

    @Test
    fun preferencesAreIsolatedPerAccount() = runTest(testDispatcher) {
        val store = InMemoryThemePreferenceStore()
        val viewModel = ThemeViewModel(store)
        advanceUntilIdle()

        viewModel.setAccount(UserId("user-1"))
        advanceUntilIdle()

        viewModel.setThemePreference(ThemePreference.DARK)
        advanceUntilIdle()

        viewModel.setAccount(UserId("user-2"))
        advanceUntilIdle()

        assertEquals(ThemePreference.SYSTEM, viewModel.themePreference.value)

        viewModel.setThemePreference(ThemePreference.LIGHT)
        advanceUntilIdle()

        viewModel.setAccount(UserId("user-1"))
        advanceUntilIdle()

        assertEquals(ThemePreference.DARK, viewModel.themePreference.value)
    }

    @Test
    fun clearingAccountRetainsLastAppliedPreference() = runTest(testDispatcher) {
        val viewModel = ThemeViewModel(InMemoryThemePreferenceStore())
        advanceUntilIdle()

        viewModel.setAccount(UserId("user-1"))
        advanceUntilIdle()

        viewModel.setThemePreference(ThemePreference.DARK)
        advanceUntilIdle()

        viewModel.setAccount(null)
        advanceUntilIdle()

        assertEquals(ThemePreference.DARK, viewModel.themePreference.value)
    }

    @Test
    fun anonymousPreferenceChangesDoNotPersist() = runTest(testDispatcher) {
        val store = InMemoryThemePreferenceStore()
        val viewModel = ThemeViewModel(store)
        advanceUntilIdle()

        viewModel.setThemePreference(ThemePreference.DARK)
        advanceUntilIdle()

        viewModel.setAccount(UserId("user-1"))
        advanceUntilIdle()

        assertEquals(ThemePreference.SYSTEM, viewModel.themePreference.value)
    }
}
