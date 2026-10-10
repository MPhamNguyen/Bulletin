package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class GetProfileOverviewTest {
    @Test
    fun overviewReturnsProfileReputationAndActivity() = runTest {
        val repository = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val profile = StudentProfile(UserId("student"), StudentEmail("student@school.edu"), "Student Name")
        repository.updateProfile(profile)
        val activity = ProfileActiveListingsProvider { 3 }
        val overview = GetProfileOverview(repository, activity)(profile.id).getOrThrow()!!
        assertEquals(profile.id, overview.profile.id)
        assertEquals(3, overview.activeListingsCount)
        assertEquals(0, overview.reputation.totalReviews)
        assertEquals(3, GetProfileActivity(activity)(profile.id))
        assertNull(GetProfileOverview(repository, activity)(UserId("missing")).getOrThrow())
    }

    @Test
    fun overviewPropagatesRepositoryFailuresWithoutLoadingActivity() = runTest {
        val error = IllegalStateException("read failed")
        val repository = object : ProfileRepository by InMemoryProfileRepository() {
            override suspend fun getProfile(userId: UserId): Result<StudentProfile?> = Result.Error(error)
        }
        var activityCalls = 0
        val activity = ProfileActiveListingsProvider {
            activityCalls++
            1
        }
        val query = GetProfileOverview(repository, activity)
        assertEquals(error, assertIs<Result.Error>(query(UserId("student"))).exception)
        assertEquals(0, activityCalls)
    }
}
