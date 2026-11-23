package com.jdrms.bulletin.core.common

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BoundedMemoryCacheInvalidationTest {
    @Test
    fun invalidationAndClearDetachInflightLookupsWithoutStrandingWaiters() = runTest {
        for (operation in listOf("invalidate", "clear", "clearWhere")) {
            val cache = BoundedMemoryCache<String, String>(2, 100, { 0L })
            val result = CompletableDeferred<String>()
            val first = async(start = CoroutineStart.UNDISPATCHED) { cache.getOrLoad("key") { result.await() } }
            val waiter = async(start = CoroutineStart.UNDISPATCHED) { cache.getOrLoad("key") { error("shared") } }
            when (operation) {
                "invalidate" -> cache.invalidate("key")
                "clear" -> cache.clear()
                else -> cache.clearWhere { it == "key" }
            }
            assertEquals("fresh", cache.getOrLoad("key") { "fresh" })
            result.complete("stale")
            assertEquals("stale", first.await())
            assertEquals("stale", waiter.await())
            assertEquals("fresh", cache.getOrLoad("key") { error("cached") })
        }
    }
}
