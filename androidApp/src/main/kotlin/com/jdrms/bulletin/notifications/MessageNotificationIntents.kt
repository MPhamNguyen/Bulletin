package com.jdrms.bulletin.notifications

import android.content.Intent
import android.net.Uri

/**
 * Intent contract between a message notification and [com.jdrms.bulletin.MainActivity].
 *
 * Android compares PendingIntents with [Intent.filterEquals], which ignores extras. The target is therefore
 * encoded in [Intent.setData] (`bulletin-notification://conversation/<recipient>/<conversation>`), so each
 * conversation has its own collision-free PendingIntent identity regardless of request code.
 */
internal object MessageNotificationIntents {
    const val ACTION_OPEN_CONVERSATION = "com.jdrms.bulletin.action.OPEN_CONVERSATION"
    const val SCHEME = "bulletin-notification"
    private const val HOST = "conversation"

    data class Target(val conversationId: String, val recipientId: String)

    /** Builds the identity URI. [Uri.Builder.appendPath] percent-encodes each segment, so IDs cannot collide. */
    fun uriFor(conversationId: String, recipientId: String): Uri = Uri.Builder()
        .scheme(SCHEME)
        .authority(HOST)
        .appendPath(recipientId)
        .appendPath(conversationId)
        .build()

    fun isNotificationIntent(intent: Intent?): Boolean =
        intent?.action == ACTION_OPEN_CONVERSATION

    fun parse(intent: Intent?): Target? {
        val data = intent?.takeIf(::isNotificationIntent)?.data
        val segments = data
            ?.takeIf { it.scheme == SCHEME && it.host == HOST }
            ?.pathSegments
            ?.takeIf { it.size == 2 }
        val recipientId = segments?.get(0)?.takeIf(String::isNotBlank)
        val conversationId = segments?.get(1)?.takeIf(String::isNotBlank)
        return if (conversationId != null && recipientId != null) Target(conversationId, recipientId) else null
    }

    /** Consumed targets must not be replayed when the Activity is recreated. */
    fun consume(intent: Intent) {
        intent.action = null
        intent.data = null
    }
}
