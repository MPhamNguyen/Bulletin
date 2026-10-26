package com.jdrms.bulletin.domain.home.presentation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.GetPersonalizedFeed
import com.jdrms.bulletin.domain.home.application.FakeSwipeDecisionRepository
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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Instant

internal fun homeItem(id: String) = HomeFeedItem(
    HomeItemId(id),
    "Listing $id",
    "Available",
    HomePrice(10.0),
    HomeFeedCategory.TEXTBOOKS
)

internal fun homePage(vararg ids: String, cursor: String? = null) = HomeFeedPage(
    ids.map(::homeItem),
    cursor?.let { HomeFeedCursor(Instant.fromEpochMilliseconds(100), HomeItemId(it)) }
)

internal class HomeDeckFixture : HomeRepository, SessionRepository {
    val responses = ArrayDeque<suspend () -> HomeFeedPage>()
    val cursors = mutableListOf<HomeFeedCursor?>()
    val decisions = FakeSwipeDecisionRepository()
    var cancellations = 0
    override val state = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    val store = ViewModelStore()
    val viewModel = ViewModelProvider.create(
        store,
        viewModelFactory {
            initializer {
                HomeViewModel(
                    GetPersonalizedFeed(this@HomeDeckFixture),
                    UpdateUserPreferences(this@HomeDeckFixture),
                    this@HomeDeckFixture,
                    swipeDecisionRepository = decisions
                )
            }
        }
    )[HomeViewModel::class]

    fun page(vararg ids: String, cursor: String? = null) {
        responses.add { homePage(*ids, cursor = cursor) }
    }

    fun signIn(id: String) {
        authenticated(StudentProfile(UserId(id), StudentEmail("$id@university.edu"), id))
    }

    override suspend fun getFeed(userId: String?, cursor: HomeFeedCursor?, pageSize: Int): HomeFeedPage {
        cursors += cursor
        return responses.removeFirst().invoke()
    }
    override suspend fun getPreferences(userId: String?) = UserPreferences()
    override suspend fun updatePreferences(
        userId: String,
        preferences: UserPreferences
    ): Result<Unit> = Result.Success(Unit)
    override suspend fun restore(): Result<StudentProfile?> = Result.Success(null)
    override fun authenticated(profile: StudentProfile) { state.value = SessionState.Authenticated(profile) }
    override fun unauthenticated() { state.value = SessionState.Unauthenticated }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun homeDeckTest(block: suspend TestScope.(HomeDeckFixture) -> Unit) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val fixture = HomeDeckFixture()
    try {
        block(fixture)
    } finally {
        fixture.store.clear()
        Dispatchers.resetMain()
    }
}
