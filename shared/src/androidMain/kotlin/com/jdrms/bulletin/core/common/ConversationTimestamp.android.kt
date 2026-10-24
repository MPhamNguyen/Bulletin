package com.jdrms.bulletin.core.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

actual fun formatConversationTimestamp(timestampMillis: Long, nowMillis: Long): String {
    if (timestampMillis <= 0L) return "Date unavailable"
    return SimpleDateFormat("h:mm a", Locale.US)
        .format(Date(timestampMillis))
        .uppercase(Locale.US)
}

actual fun formatConversationDate(timestampMillis: Long, nowMillis: Long): String {
    if (timestampMillis <= 0L) return "Date unavailable"
    val locale = Locale.US
    val date = Date(timestampMillis)
    val time = SimpleDateFormat("h:mm a", locale).format(date).uppercase(locale)
    if (weekStart(timestampMillis) == weekStart(nowMillis)) {
        val weekday = SimpleDateFormat("EEE", locale).format(date).uppercase(locale)
        return weekday
    }
    val day = Calendar.getInstance().apply { timeInMillis = timestampMillis }.get(Calendar.DAY_OF_MONTH)
    val month = SimpleDateFormat("MMMM", locale).format(date).uppercase(locale)
    return "$month $day${dayOrdinalSuffix(day)} AT $time"
}

private fun weekStart(timestampMillis: Long): Long = Calendar.getInstance().run {
    timeInMillis = timestampMillis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    val firstDay = firstDayOfWeek
    while (get(Calendar.DAY_OF_WEEK) != firstDay) add(Calendar.DAY_OF_MONTH, -1)
    timeInMillis
}

private fun dayOrdinalSuffix(day: Int): String = when {
    day % 100 in 11..13 -> "TH"
    day % 10 == 1 -> "ST"
    day % 10 == 2 -> "ND"
    day % 10 == 3 -> "RD"
    else -> "TH"
}
