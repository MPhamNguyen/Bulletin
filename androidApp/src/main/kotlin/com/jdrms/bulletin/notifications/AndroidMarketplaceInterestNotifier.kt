package com.jdrms.bulletin.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jdrms.bulletin.MainActivity
import com.jdrms.bulletin.R
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestAlert
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestNotifier

internal class AndroidMarketplaceInterestNotifier(context: Context) : MarketplaceInterestNotifier {
    private val context = context.applicationContext

    override fun show(alert: MarketplaceInterestAlert) {
        ensureChannel()
        val manager = NotificationManagerCompat.from(context)
        if (!canPost(manager)) {
            Log.i(LOG_TAG, "stage=android_post_blocked")
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_interest_title, alert.title))
            .setContentText(alert.summary)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(alert.createdAtMillis)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(openListingIntent(alert))
            .build()
        try {
            manager.notify(alert.listingId.value, NOTIFICATION_ID, notification)
            Log.i(LOG_TAG, "stage=android_posted")
        } catch (_: SecurityException) {
            Log.i(LOG_TAG, "stage=android_post_security_exception")
            // Permission was revoked after the check.
        }
    }

    override fun clearAll() {
        val system = context.getSystemService(NotificationManager::class.java) ?: return
        val manager = NotificationManagerCompat.from(context)
        system.activeNotifications.filter { it.id == NOTIFICATION_ID }.forEach { manager.cancel(it.tag, it.id) }
    }

    fun initializeChannel() = ensureChannel()

    private fun openListingIntent(alert: MarketplaceInterestAlert): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MarketplaceInterestNotificationIntents.ACTION_OPEN_LISTING
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = MarketplaceInterestNotificationIntents.uriFor(alert.listingId.value, alert.recipientId.value)
        }
        return PendingIntent.getActivity(
            context,
            0,
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
        return manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManagerCompat.IMPORTANCE_NONE
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_interests),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = context.getString(R.string.notification_channel_interests_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "marketplace_interests"
        const val NOTIFICATION_ID = 2
        const val LOG_TAG = "InterestNotifications"
    }
}
