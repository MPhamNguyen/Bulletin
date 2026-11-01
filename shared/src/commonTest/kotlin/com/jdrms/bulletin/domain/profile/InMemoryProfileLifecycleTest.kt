package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryProfileLifecycleTest {

    @Test
    fun testProfileRepositoryGetProfileReturnsResult() = runTest {
        val repo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val notFoundResult = repo.getProfile(UserId("nonexistent_user"))
        assertTrue(notFoundResult is Result.Success)
        assertNull(notFoundResult.data)

        val newProfile = StudentProfile(
            id = UserId("u_100"),
            email = StudentEmail("test@csulb.edu"),
            fullName = "Test User"
        )
        repo.updateProfile(newProfile)

        val foundResult = repo.getProfile(UserId("u_100"))
        assertTrue(foundResult is Result.Success)
        assertEquals("Test User", foundResult.data?.fullName)
    }

    @Test
    fun testInMemoryProfileRepositorySoftDelete() = runTest {
        val repo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val profile = StudentProfile(
            id = UserId("u1"),
            email = StudentEmail("student@school.edu"),
            fullName = "Student Name"
        )
        repo.updateProfile(profile)

        val result = repo.softDelete(profile.id, "2026-10-06T19:00:00Z")
        assertTrue(result is Result.Success)

        val fetched = repo.getProfile(profile.id)
        assertTrue(fetched is Result.Success)
        assertEquals("2026-10-06T19:00:00Z", fetched.data?.deletedAt)
        assertTrue(fetched.data?.isDeleted == true)
    }
}
