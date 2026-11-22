package com.jdrms.bulletin.core.common

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BoundedMemoryCachePeekTest {
    @Test
    fun cachedReadsDoNotWaitForPendingLoadsAndRespectExpiration() = runTest {
        var now = 0L
        val cache = BoundedMemoryCache<String, String>(2, 10) { now }
        val response = CompletableDeferred<String>()
        val pending = async(start = CoroutineStart.UNDISPATCHED) { cache.getOrLoad("photo") { response.await() } }
        assertNull(cache.getIfPresent("photo"))
        response.complete("url")
        pending.await()
        assertEquals("url", cache.getIfPresent("photo"))
        now = 10
        assertNull(cache.getIfPresent("photo"))
        assertNull(cache.getIfPresent("missing"))
    }
}
