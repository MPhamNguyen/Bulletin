package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileReadCacheTest {
    @Test
    fun sessionChangeClearsUserScopedReadData() = runTest {
        var loads = 0
        val cache = ProfileReadCache { 0L }
        val id = UserId("user-a")
        cache.observeSession("user-a")
        cache.reputation(id, isCurrentUser = true) {
            loads++
            StudentReputation(id, averageRating = 3.0)
        }
        assertEquals(
            3.0,
            cache.reputation(id, isCurrentUser = true) {
                loads++
                StudentReputation(id)
            }.averageRating
        )
        cache.reputation(id, isCurrentUser = false) {
            loads++
            StudentReputation(id, averageRating = 4.0)
        }
        cache.observeSession("user-b")
        assertEquals(
            0.0,
            cache.reputation(id, isCurrentUser = true) {
                loads++
                StudentReputation(id)
            }.averageRating
        )
        assertEquals(
            4.0,
            cache.reputation(id, isCurrentUser = false) {
                loads++
                StudentReputation(id)
            }.averageRating
        )
        assertEquals(3, loads)
        cache.observeSession(null)
    }
}
