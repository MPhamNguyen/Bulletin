package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.ListingDecimal

internal fun rentAmountToDigits(amount: String): String =
    ListingDecimal.parse(amount, scale = 2)?.unscaled?.toString().orEmpty()

internal fun rentDigitsToAmount(digits: String): String =
    normalizeCurrencyDigits(digits).takeIf { it.isNotEmpty() }
        ?.let { ListingDecimal(it.toLong(), scale = 2).toPlainString() }.orEmpty()
