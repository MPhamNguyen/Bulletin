package com.jdrms.bulletin.domain.listings.domain.model

/** The storage sizes a seller may pick, in GB. Mirrors the buyer-facing storage filter buckets. */
object ListingStorageOptions {
    val gigabytes = listOf(128L, 256L, 512L, 1024L, 2048L)

    fun label(gigabytes: Long): String =
        if (gigabytes >= GB_PER_TB && gigabytes % GB_PER_TB == 0L) "${gigabytes / GB_PER_TB} TB" else "$gigabytes GB"

    private const val GB_PER_TB = 1024L
}
