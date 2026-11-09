package com.jdrms.bulletin.domain.marketplace.domain.model

sealed interface MarketplaceAttributePredicate {
    data class ExactText(val value: String, val usesMatchKey: Boolean = false) : MarketplaceAttributePredicate {
        init { require(value.isNotBlank()) { "Exact text filters cannot be blank." } }
    }

    data class Choice(val code: String) : MarketplaceAttributePredicate {
        init { require(code.isNotBlank()) { "Choice filters cannot be blank." } }
    }

    data class IntegerRange(val range: MarketplaceCapacityRange) : MarketplaceAttributePredicate

    data class DecimalRange(val min: Double? = null, val max: Double? = null) : MarketplaceAttributePredicate {
        init {
            require(min != null || max != null) { "A decimal range needs a minimum or a maximum." }
            require(min == null || min.isFinite() && min >= 0.0) { "Minimum value is out of range." }
            require(max == null || max.isFinite() && max >= 0.0) { "Maximum value is out of range." }
            require(min == null || max == null || min <= max) { "Minimum value cannot exceed the maximum." }
        }
    }

    data class DateRange(val minIso: String? = null, val maxIso: String? = null) : MarketplaceAttributePredicate {
        init {
            require(minIso != null || maxIso != null) { "A date range needs a start or end date." }
            require(minIso == null || isIsoDate(minIso)) { "Start date must use a real YYYY-MM-DD date." }
            require(maxIso == null || isIsoDate(maxIso)) { "End date must use a real YYYY-MM-DD date." }
            require(minIso == null || maxIso == null || minIso <= maxIso) {
                "Start date cannot be after the end date."
            }
        }

        private companion object {
            val ISO_DATE = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$")

            fun isIsoDate(value: String): Boolean {
                val match = ISO_DATE.matchEntire(value) ?: return false
                val (yearText, monthText, dayText) = match.destructured
                val year = yearText.toInt()
                val month = monthText.toInt()
                val day = dayText.toInt()
                val maxDay = when (month) {
                    2 -> if (year % 400 == 0 || year % 4 == 0 && year % 100 != 0) 29 else 28
                    4, 6, 9, 11 -> 30
                    in 1..12 -> 31
                    else -> return false
                }
                return day in 1..maxDay
            }
        }
    }
}
