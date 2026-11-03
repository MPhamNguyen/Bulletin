package com.jdrms.bulletin.domain.listings.domain.service

import com.jdrms.bulletin.domain.listings.domain.model.ListingTextFormat

/**
 * Normalization for free-text attribute values.
 *
 * Identifiers (ISBN, course code, size) are stored in one canonical spelling. Brands keep the seller's display
 * spelling; [matchKey] is the separate, deterministic key used for equality filters.
 */
object ListingAttributeText {
    private val COURSE_CODE = Regex("^([A-Za-z]{2,5})[ -]?(\\d{2,4}[A-Za-z]{0,2})$")
    private val SIZE = Regex("^[A-Z0-9][A-Z0-9 ./-]{0,11}$")
    private val WHITESPACE = Regex("\\s+")
    private const val ISBN10_LENGTH = 10
    private const val ISBN13_LENGTH = 13
    private const val ISBN10_MODULUS = 11
    private const val ISBN13_MODULUS = 10

    /** Trims and collapses whitespace; returns null when the text has control characters or is blank. */
    fun clean(raw: String): String? {
        if (raw.any { it.isISOControl() }) return null
        return raw.trim().replace(WHITESPACE, " ").takeIf(String::isNotEmpty)
    }

    /**
     * Brand matching key: case-insensitive, with punctuation noise removed (`Levi's` equals `levis`), hyphens and
     * slashes read as spaces, `&` read as `and`, and runs of whitespace collapsed. It never merges names that
     * differ in letters or digits: `Dell` and `Dell Inc` stay distinct, as do `Pen Tel` and `Pentel`.
     */
    fun matchKey(brand: String): String {
        val spaced = brand.lowercase()
            .replace("&", " and ")
            .replace(Regex("[-_/]"), " ")
            .replace(Regex("['’.,]"), "")
        return spaced.filter { it.isLetterOrDigit() || it == ' ' }.trim().replace(WHITESPACE, " ")
    }

    /** Returns the canonical stored spelling for [raw] under [format], or null when it is not valid. */
    fun normalize(format: ListingTextFormat, raw: String): String? {
        val cleaned = clean(raw) ?: return null
        return when (format) {
            ListingTextFormat.BRAND -> cleaned.takeIf { text -> text.any(Char::isLetterOrDigit) }
            ListingTextFormat.ISBN -> normalizeIsbn(cleaned)
            ListingTextFormat.COURSE_CODE -> normalizeCourseCode(cleaned)
            ListingTextFormat.CLOTHING_SIZE -> cleaned.uppercase().takeIf { SIZE.matches(it) }
        }
    }

    /** Digits only (with a trailing `X` for ISBN-10), checksum-verified. Stored as text to keep leading zeros. */
    fun normalizeIsbn(raw: String): String? {
        val compact = raw.filterNot { it == '-' || it == ' ' }.uppercase()
        val valid = when (compact.length) {
            ISBN10_LENGTH -> isValidIsbn10(compact)
            ISBN13_LENGTH -> isValidIsbn13(compact)
            else -> false
        }
        return compact.takeIf { valid }
    }

    /** `cecs-328`, `CECS328`, and `cecs 328` all become `CECS 328`. */
    fun normalizeCourseCode(raw: String): String? {
        val match = COURSE_CODE.matchEntire(raw.trim()) ?: return null
        val (department, number) = match.destructured
        return "${department.uppercase()} ${number.uppercase()}"
    }

    private fun isValidIsbn10(isbn: String): Boolean {
        val body = isbn.dropLast(1)
        val last = isbn.last()
        if (!body.all(Char::isDigit) || !(last.isDigit() || last == 'X')) return false
        val sum = body.mapIndexed { index, digit -> (ISBN10_LENGTH - index) * digit.digitToInt() }.sum() +
            if (last == 'X') ISBN10_LENGTH else last.digitToInt()
        return sum % ISBN10_MODULUS == 0
    }

    private fun isValidIsbn13(isbn: String): Boolean {
        if (!isbn.all(Char::isDigit) || !(isbn.startsWith("978") || isbn.startsWith("979"))) return false
        val sum = isbn.mapIndexed { index, digit -> digit.digitToInt() * if (index % 2 == 0) 1 else 3 }.sum()
        return sum % ISBN13_MODULUS == 0
    }
}
