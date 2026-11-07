package com.jdrms.bulletin.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jdrms.bulletin.MainActivity
import com.jdrms.bulletin.R
import com.jdrms.bulletin.domain.messages.application.IncomingMessageAlert
import com.jdrms.bulletin.domain.messages.application.IncomingMessageNotifier
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId

/**
 * Posts one notification per conversation (tag = conversation ID) so a newer message replaces the previous one.
 * Content is redacted on the lock screen. Nothing is posted without permission or while the channel is blocked.
 */
internal class AndroidIncomingMessageNotifier(context: Context) : IncomingMessageNotifier {
    private val context = context.applicationContext

    override fun show(alert: IncomingMessageAlert) {
        ensureChannel()
        val manager = NotificationManagerCompat.from(context)
        if (!canPost(manager)) return
        val sender = alert.senderName.ifBlank { context.getString(R.string.notification_unknown_sender) }
        val publicVersion = baseBuilder(alert)
            .setContentTitle(context.getString(R.string.notification_new_message))
            .build()
        val notification = baseBuilder(alert)
            .setContentTitle(sender)
            .setContentText(alert.content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.content))
            .setPublicVersion(publicVersion)
            .build()
        try {
            manager.notify(alert.conversationId.value, NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission was revoked after the check above.
        }
    }

    override fun clear(conversationId: ConversationId) {
        NotificationManagerCompat.from(context).cancel(conversationId.value, NOTIFICATION_ID)
    }

    override fun clearAll() {
        val system = context.getSystemService(NotificationManager::class.java) ?: return
        val manager = NotificationManagerCompat.from(context)
        system.activeNotifications.filter { it.id == NOTIFICATION_ID }.forEach { manager.cancel(it.tag, it.id) }
    }

    private fun baseBuilder(alert: IncomingMessageAlert) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .setWhen(alert.createdAt.toEpochMilliseconds())
        .setShowWhen(true)
        .setAutoCancel(true)
        .setContentIntent(openConversationIntent(alert))

    private fun openConversationIntent(alert: IncomingMessageAlert): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MessageNotificationIntents.ACTION_OPEN_CONVERSATION
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MessageNotificationIntents.EXTRA_CONVERSATION_ID, alert.conversationId.value)
            putExtra(MessageNotificationIntents.EXTRA_RECIPIENT_ID, alert.recipientId.value)
        }
        // Extras do not distinguish PendingIntents, so the request code is derived from the conversation.
        // Intent.data is unavailable: MainActivity treats it as an auth callback.
        return PendingIntent.getActivity(
            context,
            alert.conversationId.value.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canPost(manager: NotificationManagerCompat): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!permissionGranted || !manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val channel = manager.getNotificationChannel(CHANNEL_ID)
        return channel == null || channel.importance != NotificationManagerCompat.IMPORTANCE_NONE
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_messages),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = context.getString(R.string.notification_channel_messages_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "messages"
        const val NOTIFICATION_ID = 1
    }
}
