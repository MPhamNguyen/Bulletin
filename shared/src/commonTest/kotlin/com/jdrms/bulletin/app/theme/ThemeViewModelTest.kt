package com.jdrms.bulletin.app.theme

import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
        val store = InMemoryThemePreferenceStore(initialLastAppliedPreference = ThemePreference.DARK)
        val viewModel = ThemeViewModel(store)
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

    @Test
    fun customColorSelectionIsRestoredPerAccount() = runTest(testDispatcher) {
        val store = InMemoryThemePreferenceStore()
        val viewModel = ThemeViewModel(store)
        advanceUntilIdle()
        viewModel.setAccount(UserId("user-1"))
        advanceUntilIdle()

        val custom = com.jdrms.bulletin.core.designsystem.theme.BulletinThemeColor(
            androidx.compose.ui.graphics.Color(0xFF123ABC).value
        )
        viewModel.setThemeColor(custom)
        advanceUntilIdle()
        viewModel.setAccount(UserId("user-2"))
        advanceUntilIdle()
        assertEquals(com.jdrms.bulletin.core.designsystem.theme.BulletinThemeColor.CLASSIC, viewModel.themeColor.value)

        viewModel.setAccount(UserId("user-1"))
        advanceUntilIdle()
        assertEquals(custom, viewModel.themeColor.value)
    }

    @Test
    fun selectingThemeColorsPersistsBaseAndAccentTogether() = runTest(testDispatcher) {
        val store = InMemoryThemePreferenceStore()
        val viewModel = ThemeViewModel(store)
        advanceUntilIdle()
        val user = UserId("user-1")
        val baseColor = com.jdrms.bulletin.core.designsystem.theme.BulletinThemeColor(
            androidx.compose.ui.graphics.Color(0xFF123456).value
        )
        val accentColor = com.jdrms.bulletin.core.designsystem.theme.BulletinThemeColor(
            androidx.compose.ui.graphics.Color(0xFFABCDEF).value
        )
        viewModel.setAccount(user)
        advanceUntilIdle()

        viewModel.setThemeColors(baseColor, accentColor)
        advanceUntilIdle()

        assertEquals(baseColor, store.baseColorFor(user))
        assertEquals(accentColor, store.themeColorFor(user))
    }

    @Test
    fun latestAccountLoadWinsWhenAnEarlierLoadIsSuspended() = runTest(testDispatcher) {
        val backingStore = InMemoryThemePreferenceStore(
            initialPreferences = mapOf(UserId("user-1") to ThemePreference.DARK)
        )
        val userOneLoad = CompletableDeferred<Unit>()
        val store = object : ThemePreferenceStores by backingStore {
            override suspend fun preferenceFor(userId: UserId): ThemePreference {
                if (userId == UserId("user-1")) userOneLoad.await()
                return backingStore.preferenceFor(userId)
            }
        }
        val viewModel = ThemeViewModel(store)
        advanceUntilIdle()

        viewModel.setAccount(UserId("user-1"))
        runCurrent()
        viewModel.setAccount(UserId("user-2"))
        advanceUntilIdle()
        userOneLoad.complete(Unit)
        advanceUntilIdle()

        assertEquals(ThemePreference.SYSTEM, viewModel.themePreference.value)
    }
}
