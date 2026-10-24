package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.core.common.Result
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow

internal fun deckItem(id: String) = HomeFeedItem(
    id = HomeItemId(id),
    title = "Listing $id",
    description = "Available",
    price = HomePrice(10.0),
    category = HomeFeedCategory.TEXTBOOKS
)

internal class DeckCharacterizationRepository(
    private val items: List<HomeFeedItem>,
    private val nextCursor: HomeFeedCursor? = null,
    private val append: CompletableDeferred<HomeFeedPage> = CompletableDeferred(HomeFeedPage(emptyList(), null))
) : HomeRepository {
    val requestedCursors = mutableListOf<HomeFeedCursor?>()

    override suspend fun getFeed(userId: String?, cursor: HomeFeedCursor?, pageSize: Int): HomeFeedPage {
        requestedCursors += cursor
        return if (cursor == null) HomeFeedPage(items, nextCursor) else append.await()
    }

    override suspend fun getPreferences(userId: String?): UserPreferences = UserPreferences()

    override suspend fun updatePreferences(userId: String, preferences: UserPreferences): Result<Unit> =
        Result.Success(Unit)
}

internal class DeckCharacterizationSession : SessionRepository {
    override val state = MutableStateFlow<SessionState>(
        SessionState.Authenticated(
            StudentProfile(UserId("home-test"), StudentEmail("home@university.edu"), "Home Test")
        )
    )

    override suspend fun restore(): Result<StudentProfile?> =
        Result.Success((state.value as? SessionState.Authenticated)?.profile)

    override fun authenticated(profile: StudentProfile) {
        state.value = SessionState.Authenticated(profile)
    }

    override fun unauthenticated() {
        state.value = SessionState.Unauthenticated
    }
}
