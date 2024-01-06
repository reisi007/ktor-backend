package pictures.reisinger

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.LocalDateTime
import kotlin.reflect.KProperty

class MemoryCacheDelegate<C>(private val ttl: Duration, private val supplier: () -> C) {
    private lateinit var cache: CacheEntry<C>;

    operator fun getValue(thisRef: Any?, property: KProperty<*>): C {
        val now = LocalDateTime.now()
        if (isCachedValid(now)) return cache.value
        synchronized(cache) {
            if (isCachedValid(now)) return cache.value
            val nextValue = supplier()
            cache = CacheEntry(nextValue, LocalDateTime.now().plus(ttl))

            return nextValue
        }
    }

    private fun isCachedValid(now: LocalDateTime): Boolean {
        return this::cache.isInitialized && cache.validUntil >= now
    }
}

class SuspendingMemoryCache<C>(private val ttl: Duration, private val supplier: suspend () -> C) {
    private lateinit var cache: CacheEntry<C>;
    private val mutex = Mutex()

    suspend fun getValue(): C {
        val now = LocalDateTime.now()
        if (isCachedValid(now)) return cache.value
        mutex.withLock {
            if (isCachedValid(now)) return cache.value
            val nextValue = supplier()
            cache = CacheEntry(nextValue, LocalDateTime.now().plus(ttl))

            return nextValue
        }
    }

    private fun isCachedValid(now: LocalDateTime): Boolean {
        return this::cache.isInitialized && cache.validUntil >= now
    }
}


private data class CacheEntry<E>(val value: E, val validUntil: LocalDateTime)
