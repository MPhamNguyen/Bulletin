package com.jdrms.bulletin.core.common

/** Formats a conversation message time using the device's local timezone. */
expect fun formatConversationTimestamp(timestampMillis: Long, nowMillis: Long): String

/** Formats a conversation day separator in the device's local timezone. */
expect fun formatConversationDate(timestampMillis: Long, nowMillis: Long): String
