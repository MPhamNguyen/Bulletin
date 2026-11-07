package com.jdrms.bulletin.notifications

import android.content.Intent

/** Intent contract between a message notification and [com.jdrms.bulletin.MainActivity]. */
internal object MessageNotificationIntents {
    const val ACTION_OPEN_CONVERSATION = "com.jdrms.bulletin.action.OPEN_CONVERSATION"
    const val EXTRA_CONVERSATION_ID = "com.jdrms.bulletin.extra.CONVERSATION_ID"
    const val EXTRA_RECIPIENT_ID = "com.jdrms.bulletin.extra.RECIPIENT_ID"

    data class Target(val conversationId: String, val recipientId: String)

    fun parse(intent: Intent?): Target? {
        if (intent?.action != ACTION_OPEN_CONVERSATION) return null
        val conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)?.takeIf(String::isNotBlank)
        val recipientId = intent.getStringExtra(EXTRA_RECIPIENT_ID)?.takeIf(String::isNotBlank)
        return if (conversationId != null && recipientId != null) Target(conversationId, recipientId) else null
    }

    /** Consumed targets must not be replayed when the Activity is recreated. */
    fun consume(intent: Intent) {
        intent.action = null
        intent.removeExtra(EXTRA_CONVERSATION_ID)
        intent.removeExtra(EXTRA_RECIPIENT_ID)
    }
}
