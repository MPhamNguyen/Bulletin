package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.domain.profile.application.GetProfileActivity
import com.jdrms.bulletin.domain.profile.application.GetProfileOverview
import com.jdrms.bulletin.domain.profile.application.ProfileActiveListingsProvider
import com.jdrms.bulletin.domain.profile.application.ProfileSoldListingsProvider
import com.jdrms.bulletin.domain.profile.domain.model.Rating
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileReviewRefreshTest {
    @Test
    fun refreshUpdatesReviewsAndStatsEvenWhenReviewCountIsUnchanged() = profileTest {
        val fixture = ProfileFixture()
        val profile = fixture.authenticate()
        var review = StudentReview(
            ReviewId("review"),
            "buyer",
            "Buyer",
            profile.id,
            Rating(5),
            "Original comment",
            reviewerAvatarUrl = "https://example.com/avatar.jpg"
        )
        val repository = object : ProfileRepository by fixture.profiles {
            override suspend fun getReputation(userId: UserId) = StudentReputation(
                userId,
                review.rating.score.toDouble(),
                1,
                listOf(review)
            )
        }
        var active = 2
        var sold = 1
        val activity = ProfileActiveListingsProvider { active }
        val viewModel = ProfileViewModel(
            fixture.session,
            GetProfileOverview(repository, activity),
            GetProfileActivity(activity),
            RefreshSignal(),
            soldListingsProvider = ProfileSoldListingsProvider { sold }
        )
        advanceUntilIdle()
        assertEquals(listOf(review), viewModel.uiState.value.reputation?.reviews)

        review = review.copy(comment = "Updated comment", rating = Rating(4))
        active = 1
        sold = 2
        viewModel.refreshProfile()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(review), state.reputation?.reviews)
        assertEquals(1, state.reputation?.totalReviews)
        assertEquals(4.0, state.reputation?.averageRating)
        assertEquals(1, state.activeListingsCount)
        assertEquals(2, state.itemsSoldCount)
    }
}
