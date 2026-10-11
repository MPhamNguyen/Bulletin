package com.jdrms.bulletin.core.common

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BoundedMemoryCacheTest {
    @Test
    fun cacheHitAvoidsLoadingAgain() = runTest {
        var loads = 0
        val cache = BoundedMemoryCache<String, String>(2, 10, { 0L })
        assertEquals(
            "value",
            cache.getOrLoad("key") {
                loads++
                "value"
            }
        )
        assertEquals(
            "value",
            cache.getOrLoad("key") {
                loads++
                "other"
            }
        )
        assertEquals(1, loads)
    }

    @Test
    fun ttlExpiryReloadsExpiredEntry() = runTest {
        var now = 0L
        var loads = 0
        val cache = BoundedMemoryCache<String, String>(2, 10, { now })
        cache.getOrLoad("key") { "first" }
        now = 10L
        assertEquals(
            "second",
            cache.getOrLoad("key") {
                loads++
                "second"
            }
        )
        assertEquals(1, loads)
    }

    @Test
    fun leastRecentlyUsedEntryIsEvictedAtCapacity() = runTest {
        val cache = BoundedMemoryCache<String, String>(2, 100, { 0L })
        cache.put("a", "A")
        cache.put("b", "B")
        cache.getOrLoad("a") { "new A" }
        cache.put("c", "C")
        assertEquals("A", cache.getOrLoad("a") { "new A" })
        assertEquals("new B", cache.getOrLoad("b") { "new B" })
    }

    @Test
    fun failedLoadLeavesExistingCacheStateUntouched() = runTest {
        val cache = BoundedMemoryCache<String, String>(2, 100, { 0L })
        cache.put("key", "original")
        assertEquals("original", cache.getOrLoad("key") { error("must not load") })
        assertFailsWith<IllegalStateException> { cache.getOrLoad("missing") { error("failure") } }
        assertEquals("original", cache.getOrLoad("key") { "replacement" })
        assertNull(cache.getOrLoad("missing") { null })
    }
}
