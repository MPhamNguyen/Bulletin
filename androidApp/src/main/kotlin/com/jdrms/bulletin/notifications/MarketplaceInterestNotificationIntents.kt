package com.jdrms.bulletin.notifications

import android.content.Intent
import android.net.Uri

internal object MarketplaceInterestNotificationIntents {
    const val ACTION_OPEN_LISTING = "com.jdrms.bulletin.action.OPEN_INTEREST_LISTING"
    private const val SCHEME = "bulletin-notification"
    private const val HOST = "interest-listing"

    data class Target(val listingId: String, val recipientId: String)

    fun uriFor(listingId: String, recipientId: String): Uri = Uri.Builder()
        .scheme(SCHEME)
        .authority(HOST)
        .appendPath(recipientId)
        .appendPath(listingId)
        .build()

    fun isNotificationIntent(intent: Intent?): Boolean = intent?.action == ACTION_OPEN_LISTING

    fun parse(intent: Intent?): Target? {
        val segments = intent?.takeIf(::isNotificationIntent)?.data
            ?.takeIf { it.scheme == SCHEME && it.host == HOST }
            ?.pathSegments
            ?.takeIf { it.size == 2 }
        val recipientId = segments?.get(0)?.takeIf(String::isNotBlank)
        val listingId = segments?.get(1)?.takeIf(String::isNotBlank)
        return if (listingId != null && recipientId != null) Target(listingId, recipientId) else null
    }

    fun consume(intent: Intent) {
        intent.action = null
        intent.data = null
    }
}
