package com.jdrms.bulletin.core.common

private const val SECOND_MILLIS = 1_000L
private const val MINUTE_MILLIS = 60 * SECOND_MILLIS
private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
private const val DAY_MILLIS = 24 * HOUR_MILLIS
private const val MONTH_MILLIS = 30 * DAY_MILLIS
private const val YEAR_MILLIS = 365 * DAY_MILLIS

fun formatRelativeTime(timestampMillis: Long, nowMillis: Long = currentTimeMillis()): String {
    if (timestampMillis <= 0L) return "Date unavailable"
    val elapsedMillis = (nowMillis - timestampMillis).coerceAtLeast(0L)
    return when {
        elapsedMillis < MINUTE_MILLIS -> "just now"
        elapsedMillis < HOUR_MILLIS -> elapsedLabel(elapsedMillis / MINUTE_MILLIS, "min")
        elapsedMillis < DAY_MILLIS -> elapsedLabel(elapsedMillis / HOUR_MILLIS, "hour")
        elapsedMillis < MONTH_MILLIS -> elapsedLabel(elapsedMillis / DAY_MILLIS, "day")
        elapsedMillis < YEAR_MILLIS -> elapsedLabel(elapsedMillis / MONTH_MILLIS, "month")
        else -> elapsedLabel(elapsedMillis / YEAR_MILLIS, "year")
    }
}

private fun elapsedLabel(amount: Long, unit: String): String {
    val suffix = if (amount == 1L) "" else "s"
    return "$amount $unit$suffix ago"
}
