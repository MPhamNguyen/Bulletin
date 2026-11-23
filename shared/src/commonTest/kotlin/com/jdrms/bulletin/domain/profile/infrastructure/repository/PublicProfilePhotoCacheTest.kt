package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PublicProfilePhotoCacheTest {
    @Test
    fun cachesMissingPhotosUntilFiveMinuteExpiry() = runTest {
        var now = 0L
        val cache = PublicProfilePhotoCache { now }
        val id = UserId("student")
        var calls = 0
        suspend fun lookup() = cache.getOrLoad(id) {
            calls++
            null
        }
        assertNull(lookup())
        now = PublicProfilePhotoCache.TTL_MILLIS - 1
        assertNull(lookup())
        assertEquals(1, calls)
        now++
        assertNull(lookup())
        assertEquals(2, calls)
    }

    @Test
    fun simultaneousLookupsShareOneRequest() = runTest {
        val cache = PublicProfilePhotoCache()
        val started = CompletableDeferred<Unit>()
        val result = CompletableDeferred<String>()
        var calls = 0
        val first = async {
            cache.getOrLoad(UserId("student")) {
                calls++
                started.complete(Unit)
                result.await()
            }
        }
        started.await()
        val second = async(start = CoroutineStart.UNDISPATCHED) {
            cache.getOrLoad(UserId("student")) {
                calls++
                "unexpected"
            }
        }
        result.complete("avatar")
        assertEquals("avatar", first.await())
        assertEquals("avatar", second.await())
        assertEquals(1, calls)
    }

    @Test
    fun errorsAreRetriedAndOtherUsersHaveSeparateEntries() = runTest {
        val cache = PublicProfilePhotoCache()
        val id = UserId("student")
        assertFailsWith<IllegalStateException> { cache.getOrLoad(id) { error("offline") } }
        assertEquals("avatar", cache.getOrLoad(id) { "avatar" })
        assertEquals("other", cache.getOrLoad(UserId("other")) { "other" })
        assertEquals("avatar", cache.getOrLoad(id) { error("cached") })
    }

    @Test
    fun cacheEvictsAfterTwoHundredUsers() = runTest {
        val cache = PublicProfilePhotoCache()
        repeat(PublicProfilePhotoCache.MAX_ENTRIES + 1) { index ->
            cache.getOrLoad(UserId("$index")) { "avatar-$index" }
        }
        assertEquals("reloaded", cache.getOrLoad(UserId("0")) { "reloaded" })
        assertEquals("avatar-200", cache.getOrLoad(UserId("200")) { error("cached") })
    }

    @Test
    fun profileChangesInvalidateOnlyThatUsersPhotoAndSessionChangesClearAll() = runTest {
        val reads = ProfileReadCache { 0L }
        val id = UserId("student")
        val other = UserId("other")
        reads.observeSession("account-a")
        reads.publicPhotos.getOrLoad(id) { "old" }
        reads.publicPhotos.getOrLoad(other) { "other" }
        reads.invalidate(id)
        assertEquals("new", reads.publicPhotos.getOrLoad(id) { "new" })
        assertEquals("other", reads.publicPhotos.getOrLoad(other) { error("cached") })
        reads.observeSession("account-a")
        assertEquals("new", reads.publicPhotos.getOrLoad(id) { error("cached") })
        reads.observeSession("account-b")
        assertEquals("new-session", reads.publicPhotos.getOrLoad(id) { "new-session" })
        reads.clearCurrentSession()
        assertNull(reads.publicPhotos.getOrLoad(id) { null })
    }
}
