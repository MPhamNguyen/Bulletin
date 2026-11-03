package com.jdrms.bulletin.domain.listings.domain.service

/** Shape limits. The Supabase trigger in `supabase/migrations` enforces the same numbers server side. */
object ListingAttributeLimits {
    const val MAX_ATTRIBUTE_COUNT = 16
    const val MAX_KEY_LENGTH = 40
    const val MAX_TEXT_LENGTH = 60
    const val MAX_PAYLOAD_BYTES = 2_048
    val KEY_PATTERN = Regex("^[a-z][a-z0-9_]*$")
}
