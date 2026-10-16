package com.jdrms.bulletin.core.common

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** A coroutine-safe, in-memory LRU cache with expiration and per-key load de-duplication. */
internal class BoundedMemoryCache<K : Any, V : Any>(
    private val maxEntries: Int,
    private val ttlMillis: Long,
    private val currentTimeMillis: () -> Long
) {
    private val mutex = Mutex()
    private val entries = linkedMapOf<K, Entry<V>>()
    private val pending = mutableMapOf<K, CompletableDeferred<V?>>()
    private var generation = 0L

    init {
        require(maxEntries > 0)
        require(ttlMillis > 0)
    }

    suspend fun getOrLoad(key: K, loader: suspend () -> V?): V? {
        var owner = false
        var loadGeneration = 0L
        val deferred = mutex.withLock {
            getLocked(key)?.let { return it }
            pending[key] ?: CompletableDeferred<V?>().also {
                pending[key] = it
                owner = true
                loadGeneration = generation
            }
        }
        if (!owner) return deferred.await()

        val result = runCatching { loader() }
        withContext(NonCancellable) {
            mutex.withLock {
                result.onSuccess { value ->
                    if (value != null && generation == loadGeneration) putLocked(key, value)
                    if (pending[key] === deferred) pending.remove(key)?.complete(value)
                }.onFailure { error ->
                    if (pending[key] === deferred) pending.remove(key)?.completeExceptionally(error)
                }
            }
        }
        return result.getOrThrow()
    }

    suspend fun put(key: K, value: V) = mutex.withLock { putLocked(key, value) }

    suspend fun invalidate(key: K) {
        mutex.withLock {
            generation++
            entries.remove(key)
        }
    }

    suspend fun clear() = mutex.withLock {
        generation++
        entries.clear()
    }

    suspend fun clearWhere(predicate: (K) -> Boolean) = mutex.withLock {
        generation++
        entries.keys.filter(predicate).forEach(entries::remove)
    }

    private fun getLocked(key: K): V? {
        val entry = entries.remove(key) ?: return null
        if (currentTimeMillis() >= entry.expiresAtMillis) return null
        entries[key] = entry
        return entry.value
    }

    private fun putLocked(key: K, value: V) {
        entries.remove(key)
        entries[key] = Entry(value, currentTimeMillis() + ttlMillis)
        while (entries.size > maxEntries) entries.remove(entries.keys.first())
    }

    private data class Entry<V>(val value: V, val expiresAtMillis: Long)
}
