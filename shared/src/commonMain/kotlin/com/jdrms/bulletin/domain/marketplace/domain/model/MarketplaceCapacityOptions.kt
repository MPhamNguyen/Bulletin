package com.jdrms.bulletin.domain.marketplace.domain.model

/** Fixed buyer-facing capacity buckets, measured in GB like listing attributes. */
object MarketplaceCapacityOptions {
    val ram = listOf(4L, 8L, 16L, 32L, 64L, 128L).toBuckets()
    val storage = listOf(128L, 256L, 512L, 1024L, 2048L).toBuckets()

    fun forKey(key: String): List<MarketplaceCapacityRange> = when (key) {
        MarketplaceAttributeCatalog.RAM_GB -> ram
        MarketplaceAttributeCatalog.STORAGE_GB -> storage
        else -> emptyList()
    }

    fun label(range: MarketplaceCapacityRange): String {
        val value = requireNotNull(range.min)
        val capacity = if (value >= 1024L) "${value / 1024L} TB" else "$value GB"
        return capacity + if (range.max == null) "+" else ""
    }

    private fun List<Long>.toBuckets() = map { value ->
        MarketplaceCapacityRange(value, value.takeUnless { it == last() })
    }
}
