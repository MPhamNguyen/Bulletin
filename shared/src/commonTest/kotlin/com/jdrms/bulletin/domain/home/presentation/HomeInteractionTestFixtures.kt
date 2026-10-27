@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.jdrms.bulletin.domain.home.presentation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.ContactSellerPort
import com.jdrms.bulletin.domain.home.application.GetPersonalizedFeed
import com.jdrms.bulletin.domain.home.application.RecordSwipeDecision
import com.jdrms.bulletin.domain.home.application.SaveListingPort
import com.jdrms.bulletin.domain.home.application.SendQuickMessage
import com.jdrms.bulletin.domain.home.application.UpdateUserPreferences
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCursor
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedPage
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SellerMessage
import com.jdrms.bulletin.domain.home.domain.model.SwipeDecision
import com.jdrms.bulletin.domain.home.domain.model.UserPreferences
import com.jdrms.bulletin.domain.home.domain.repository.HomeRepository
import com.jdrms.bulletin.domain.home.domain.repository.SwipeDecisionRepository
import com.jdrms.bulletin.domain.home.infrastructure.repository.InMemorySwipeDecisionRepository
import com.jdrms.bulletin.domain.profile.application.SessionRepository
import com.jdrms.bulletin.domain.profile.application.SessionState
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

internal class InteractionDecisions(
    private val backing: SwipeDecisionRepository = InMemorySwipeDecisionRepository()
) : SwipeDecisionRepository by backing {
    val records = mutableListOf<Pair<String, SwipeDecision>>()
    val removals = mutableListOf<Pair<String, HomeItemId>>()
    var recordFailure: Throwable? = null
    var removeFailure: Throwable? = null
    var beforeRemove: suspend () -> Unit = {}

    override suspend fun record(userId: String, decision: SwipeDecision): Result<Unit> {
        records += userId to decision
        return recordFailure?.let { Result.Error(it) } ?: backing.record(userId, decision)
    }
    override suspend fun removeSave(userId: String, listingId: HomeItemId): Result<Unit> {
        removals += userId to listingId
        beforeRemove()
        return removeFailure?.let { Result.Error(it) } ?: backing.removeSave(userId, listingId)
    }
}

internal class HomeInteractionFixture : HomeRepository, SessionRepository, SaveListingPort, ContactSellerPort {
    override val state = MutableStateFlow<SessionState>(SessionState.Authenticated(profile("viewer")))
    val decisions = InteractionDecisions()
    val signal = RefreshSignal()
    val saves = mutableListOf<Pair<String, HomeItemId>>()
    val sends = mutableListOf<Triple<String, HomeItemId, String>>()
    val cursors = mutableListOf<HomeFeedCursor?>()
    var saveResult: suspend () -> Result<Unit> = { Result.Success(Unit) }
    var sendResult: suspend () -> Result<Unit> = { Result.Success(Unit) }
    var feedResult: suspend () -> HomeFeedPage = { homePage("a", "b", "c") }
    val store = ViewModelStore()
    val vm: HomeViewModel by lazy { createVm(store) }
    val interaction get() = vm.uiState.value.interaction

    fun createVm(owner: ViewModelStore) = ViewModelProvider.create(
        owner,
        viewModelFactory {
            initializer {
                HomeViewModel(
                    GetPersonalizedFeed(this@HomeInteractionFixture),
                    UpdateUserPreferences(this@HomeInteractionFixture),
                    this@HomeInteractionFixture,
                    signal,
                    decisions,
                    RecordSwipeDecision(decisions),
                    SendQuickMessage(this@HomeInteractionFixture),
                    this@HomeInteractionFixture
                )
            }
        }
    )[HomeViewModel::class]

    fun signIn(id: String) = authenticated(profile(id))
    fun exit() = vm.onExitCompleted(requireNotNull(interaction.departure).id)
    override suspend fun save(userId: String, listingId: HomeItemId): Result<Unit> {
        saves += userId to listingId
        return saveResult()
    }
    override suspend fun send(userId: String, listingId: HomeItemId, message: SellerMessage): Result<Unit> {
        sends += Triple(userId, listingId, message.text)
        return sendResult()
    }
    override suspend fun getFeed(userId: String?, cursor: HomeFeedCursor?, pageSize: Int): HomeFeedPage {
        cursors += cursor
        return feedResult()
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

private fun profile(id: String) = StudentProfile(UserId(id), StudentEmail("$id@university.edu"), id)

internal fun interactionTest(block: suspend TestScope.(HomeInteractionFixture) -> Unit) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val fixture = HomeInteractionFixture()
    try {
        fixture.vm
        advanceUntilIdle()
        block(fixture)
    } finally {
        fixture.store.clear()
        Dispatchers.resetMain()
    }
}
