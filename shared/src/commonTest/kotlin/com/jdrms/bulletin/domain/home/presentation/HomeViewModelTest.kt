package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.GetPersonalizedFeed
import com.jdrms.bulletin.domain.home.application.UpdateUserPreferences
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCategory
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCursor
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedPage
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.HomePrice
import com.jdrms.bulletin.domain.home.domain.model.UserPreferences
import com.jdrms.bulletin.domain.home.domain.repository.HomeRepository
import com.jdrms.bulletin.domain.profile.application.SessionRepository
import com.jdrms.bulletin.domain.profile.application.SessionState
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class HomeViewModelTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun listing_visibility_invalidation_reloads_the_feed_from_the_first_page() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = PagedHomeRepository(
                listOf(
                    HomeFeedPage(listOf(item("before-change")), null),
                    HomeFeedPage(listOf(item("after-change")), null)
                )
            )
            val signal = RefreshSignal()
            val viewModel = HomeViewModel(
                GetPersonalizedFeed(repository),
                UpdateUserPreferences(repository),
                TestSessionRepository(),
                signal
            )
            advanceUntilIdle()

            signal.emit()
            advanceUntilIdle()

            assertEquals(listOf<HomeFeedCursor?>(null, null), repository.cursors)
            assertEquals(listOf("after-change"), viewModel.uiState.value.feed.map { it.id.value })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun loads_ten_listings_and_appends_next_page_once() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val nextCursor = HomeFeedCursor(Instant.fromEpochMilliseconds(100), HomeItemId("same"))
            val repository = PagedHomeRepository(
                listOf(
                    HomeFeedPage(listOf(item("same")), nextCursor),
                    HomeFeedPage(listOf(item("same"), item("next")), null)
                )
            )
            val viewModel = HomeViewModel(
                GetPersonalizedFeed(repository),
                UpdateUserPreferences(repository),
                TestSessionRepository()
            )
            advanceUntilIdle()

            viewModel.loadNextPage()
            viewModel.loadNextPage()
            advanceUntilIdle()

            assertEquals(10, repository.pageSizes.first())
            assertEquals(listOf(null, nextCursor), repository.cursors)
            assertEquals(listOf("same", "next"), viewModel.uiState.value.feed.map { it.id.value })
            assertEquals(false, viewModel.uiState.value.hasMore)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun feed_uses_the_authenticated_session_identity() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = PagedHomeRepository(listOf(HomeFeedPage(emptyList(), null)))
            val session = TestSessionRepository().apply {
                authenticated(
                    StudentProfile(
                        id = UserId("student-42"),
                        email = StudentEmail("student@university.edu"),
                        fullName = "Student"
                    )
                )
            }

            HomeViewModel(GetPersonalizedFeed(repository), UpdateUserPreferences(repository), session)
            advanceUntilIdle()

            assertEquals(listOf<String?>("student-42"), repository.userIds)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun failed_page_keeps_cursor_available_for_retry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val cursor = HomeFeedCursor(Instant.fromEpochMilliseconds(100), HomeItemId("first"))
            val repository = PagedHomeRepository(
                pages = listOf(
                    HomeFeedPage(listOf(item("first")), cursor),
                    HomeFeedPage(listOf(item("second")), null)
                ),
                failingRequestIndexes = setOf(1)
            )
            val viewModel = HomeViewModel(
                GetPersonalizedFeed(repository),
                UpdateUserPreferences(repository),
                TestSessionRepository()
            )
            advanceUntilIdle()

            viewModel.loadNextPage()
            advanceUntilIdle()
            assertEquals(cursor, viewModel.uiState.value.nextCursor)
            assertEquals(true, viewModel.uiState.value.hasMore)
            assertEquals(false, viewModel.uiState.value.isLoadingMore)

            viewModel.loadNextPage()
            advanceUntilIdle()
            assertEquals(listOf("first", "second"), viewModel.uiState.value.feed.map { it.id.value })
            assertEquals(false, viewModel.uiState.value.hasMore)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun failed_first_page_exposes_error_and_can_be_retried() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = PagedHomeRepository(
                pages = listOf(HomeFeedPage(listOf(item("recovered")), null)),
                failingRequestIndexes = setOf(0)
            )
            val viewModel = HomeViewModel(
                GetPersonalizedFeed(repository),
                UpdateUserPreferences(repository),
                TestSessionRepository()
            )
            advanceUntilIdle()

            assertEquals("Failed to load listings", viewModel.uiState.value.errorMessage)
            viewModel.loadFeed()
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.errorMessage)
            assertEquals(listOf("recovered"), viewModel.uiState.value.feed.map { it.id.value })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun position_is_owned_by_state_and_resets_on_reload_and_account_change() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val page = HomeFeedPage(listOf(item("first"), item("second")), null)
            val repository = PagedHomeRepository(List(3) { page })
            val session = TestSessionRepository()
            val viewModel = HomeViewModel(GetPersonalizedFeed(repository), UpdateUserPreferences(repository), session)
            advanceUntilIdle()
            assertEquals(0, viewModel.uiState.value.currentIndex)
            viewModel.advanceCard(0)
            viewModel.advanceCard(0)
            assertEquals(1, viewModel.uiState.value.currentIndex)
            viewModel.loadFeed()
            assertEquals(0, viewModel.uiState.value.currentIndex)
            advanceUntilIdle()
            viewModel.advanceCard(0)
            session.authenticated(StudentProfile(UserId("other"), StudentEmail("other@university.edu"), "Other"))
            advanceUntilIdle()
            assertEquals(0, viewModel.uiState.value.currentIndex)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun item(id: String) = HomeFeedItem(
        id = HomeItemId(id),
        title = "Listing $id",
        description = "Available",
        price = HomePrice(10.0),
        category = HomeFeedCategory.TEXTBOOKS,
        createdAtMillis = 100
    )
}

private class PagedHomeRepository(
    private val pages: List<HomeFeedPage>,
    private val failingRequestIndexes: Set<Int> = emptySet()
) : HomeRepository {
    val cursors = mutableListOf<HomeFeedCursor?>()
    val pageSizes = mutableListOf<Int>()
    val userIds = mutableListOf<String?>()

    override suspend fun getFeed(userId: String?, cursor: HomeFeedCursor?, pageSize: Int): HomeFeedPage {
        val requestIndex = cursors.size
        userIds += userId
        cursors += cursor
        pageSizes += pageSize
        if (requestIndex in failingRequestIndexes) error("Temporary failure")
        val pageIndex = requestIndex - failingRequestIndexes.count { it < requestIndex }
        return pages[pageIndex]
    }

    override suspend fun getPreferences(userId: String?): UserPreferences = UserPreferences()

    override suspend fun updatePreferences(userId: String, preferences: UserPreferences): Result<Unit> =
        Result.Success(Unit)
}

private class TestSessionRepository : SessionRepository {
    override val state = MutableStateFlow<SessionState>(SessionState.Unauthenticated)

    override suspend fun restore(): Result<StudentProfile?> = Result.Success(null)
    override fun authenticated(profile: StudentProfile) {
        state.value = SessionState.Authenticated(profile)
    }
    override fun unauthenticated() {
        state.value = SessionState.Unauthenticated
    }
}
