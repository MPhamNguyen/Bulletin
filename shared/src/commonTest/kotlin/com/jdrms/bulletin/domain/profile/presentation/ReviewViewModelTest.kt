package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.domain.profile.application.PublishStudentReview
import com.jdrms.bulletin.domain.profile.application.SubmitStudentReview
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {
    @Test
    fun reviewUsesAuthenticatedIdentityAndRejectsSelfReview() = profileTest {
        val fixture = ProfileFixture()
        val reviewer = fixture.authenticate()
        val publish = PublishStudentReview(
            SubmitStudentReview(fixture.profiles),
            newId = { ReviewId("review-test") },
            nowMillis = { 123L }
        )
        val vm = ReviewViewModel(fixture.session, publish)
        vm.begin(reviewer.id)
        vm.changeComment("Great exchange")
        vm.submit {}
        advanceUntilIdle()
        assertEquals("Users cannot review themselves.", vm.uiState.value.errorMessage)

        val target = UserId("seller")
        vm.begin(target)
        vm.changeComment("Great exchange")
        var submitted = false
        vm.submit { submitted = true }
        advanceUntilIdle()
        assertTrue(submitted)
        val review = fixture.profiles.getReputation(target).reviews.single()
        assertEquals(reviewer.id.value, review.reviewerId)
        assertEquals(reviewer.fullName, review.reviewerName)
        assertEquals(123L, review.createdAtMillis)
    }
}
