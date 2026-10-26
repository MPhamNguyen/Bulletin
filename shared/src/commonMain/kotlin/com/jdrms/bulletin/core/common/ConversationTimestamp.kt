package com.jdrms.bulletin.core.common

/** Formats a conversation message time using the device's local timezone. */
expect fun formatConversationTimestamp(timestampMillis: Long, nowMillis: Long): String

/** Formats a conversation day separator in the device's local timezone. */
expect fun formatConversationDate(timestampMillis: Long, nowMillis: Long): String

/** Returns a stable key for the device-local calendar day containing the timestamp. */
expect fun conversationLocalDayKey(timestampMillis: Long): Int
