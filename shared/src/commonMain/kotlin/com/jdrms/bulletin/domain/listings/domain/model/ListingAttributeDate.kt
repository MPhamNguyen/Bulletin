package com.jdrms.bulletin.domain.listings.domain.model

/** Calendar date without a time zone, stored and compared as ISO `YYYY-MM-DD`. */
data class ListingAttributeDate(val year: Int, val month: Int, val day: Int) : Comparable<ListingAttributeDate> {
    init {
        require(year in 0..9999) { "Year must have four digits." }
        require(month in 1..MONTHS) { "Month must be between 1 and 12." }
        require(day in 1..daysIn(year, month)) { "Day is not valid for that month." }
    }

    override fun compareTo(other: ListingAttributeDate): Int =
        compareValuesBy(this, other, ListingAttributeDate::year, ListingAttributeDate::month, ListingAttributeDate::day)

    /** ISO 8601 date text such as `2026-09-01`. */
    override fun toString(): String =
        "${year.toString().padStart(YEAR_DIGITS, '0')}-${month.toString().padStart(2, '0')}-" +
            day.toString().padStart(2, '0')

    companion object {
        private const val MONTHS = 12
        private const val YEAR_DIGITS = 4
        private val ISO_DATE = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$")

        fun parse(text: String): ListingAttributeDate? {
            val match = ISO_DATE.matchEntire(text.trim()) ?: return null
            val (year, month, day) = match.destructured
            return runCatching { ListingAttributeDate(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()
        }

        private fun daysIn(year: Int, month: Int): Int = when (month) {
            2 -> if (isLeapYear(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

        private fun isLeapYear(year: Int): Boolean = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    }
}
