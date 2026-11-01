package com.jdrms.bulletin.domain.listings.domain.model

/**
 * Exact fixed-scale decimal stored as an unscaled integer, so money and measurements never pass through
 * binary floating point. `ListingDecimal.parse("1250.5", scale = 2)` is 125050 with scale 2.
 */
data class ListingDecimal(val unscaled: Long, val scale: Int) : Comparable<ListingDecimal> {
    init {
        require(scale in 0..MAX_SCALE) { "Decimal scale must be between 0 and $MAX_SCALE." }
    }

    override fun compareTo(other: ListingDecimal): Int {
        require(scale == other.scale) { "Decimals must share a scale to be compared." }
        return unscaled.compareTo(other.unscaled)
    }

    /** Plain decimal text with exactly [scale] fractional digits, for example `1250.50`. */
    fun toPlainString(): String {
        if (scale == 0) return unscaled.toString()
        val negative = unscaled < 0
        val digits = unscaled.toString().removePrefix("-").padStart(scale + 1, '0')
        val whole = digits.substring(0, digits.length - scale)
        val fraction = digits.substring(digits.length - scale)
        return (if (negative) "-" else "") + whole + "." + fraction
    }

    override fun toString(): String = toPlainString()

    companion object {
        const val MAX_SCALE = 4
        private const val MAX_DIGITS = 15

        /**
         * Parses unsigned plain decimal text. Extra fractional digits are accepted only when they are zeros,
         * so `12.500` is valid at scale 2 but `12.505` is not. Exponents, signs, and separators are rejected.
         */
        fun parse(text: String, scale: Int): ListingDecimal? {
            val trimmed = text.trim()
            if (trimmed.isEmpty() || trimmed.count { it == '.' } > 1) return null
            val whole = trimmed.substringBefore('.')
            val fraction = if ('.' in trimmed) trimmed.substringAfter('.') else ""
            val hasDigits = whole.isNotEmpty() || fraction.isNotEmpty()
            if (!hasDigits || !whole.all(Char::isAsciiDigit) || !fraction.all(Char::isAsciiDigit)) {
                return null
            }
            val significant = fraction.trimEnd('0')
            if (scale !in 0..MAX_SCALE || significant.length > scale) return null
            val padded = significant.padEnd(scale, '0')
            val digits = (whole + padded).trimStart('0').ifEmpty { "0" }
            return digits.takeIf { it.length <= MAX_DIGITS }?.let { ListingDecimal(it.toLong(), scale) }
        }

        fun ofWhole(value: Long, scale: Int): ListingDecimal {
            var unscaled = value
            repeat(scale) { unscaled *= 10 }
            return ListingDecimal(unscaled, scale)
        }
    }
}

private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
