package com.jdrms.bulletin.domain.listings.presentation

private const val ISBN_INPUT_MAX_LENGTH = 13
private const val ISBN10_INPUT_LENGTH = 10
private const val ISBN13_INPUT_LENGTH = 13

private val ISBN10_FORMAT = Regex("^\\d{9}[\\dX]$")
private val ISBN13_FORMAT = Regex("^(?:978|979)\\d{10}$")

/** Removes common separators, keeps digits and a final ISBN-10 X, and caps input at thirteen characters. */
internal fun formatIsbnInput(raw: String): String {
    val allowed = raw.uppercase().filter { it in '0'..'9' || it == 'X' }
    val lastSignificant = allowed.lastIndex
    return buildString {
        allowed.forEachIndexed { index, char ->
            when {
                char in '0'..'9' && length < ISBN_INPUT_MAX_LENGTH -> append(char)
                char == 'X' && index == lastSignificant && length == ISBN10_INPUT_LENGTH - 1 -> append(char)
            }
        }
    }
}

/** Returns a format error only after the entry is long enough to represent a complete ISBN. */
internal fun isbnInputError(value: String): String? {
    if (value.length < ISBN10_INPUT_LENGTH) return null
    return if (ISBN10_FORMAT.matches(value) || ISBN13_FORMAT.matches(value)) {
        null
    } else {
        "Use 10 characters for ISBN-10, or 978/979 followed by 10 digits for ISBN-13."
    }
}

/** Counts accepted characters before the original cursor to preserve its position after formatting. */
internal fun formattedIsbnCursorOffset(rawBeforeCursor: String): Int =
    formatIsbnInput(rawBeforeCursor).length.coerceAtMost(ISBN13_INPUT_LENGTH)
