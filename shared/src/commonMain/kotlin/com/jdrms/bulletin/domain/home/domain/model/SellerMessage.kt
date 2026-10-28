package com.jdrms.bulletin.domain.home.domain.model

import com.jdrms.bulletin.core.common.Result
import kotlin.jvm.JvmInline

/**
 * Trimmed, non-blank seller message. The limit is [MAX_LENGTH] UTF-16 code units,
 * measured by Kotlin [String.length] after trimming; UI limits must use the same measure.
 */
@JvmInline
value class SellerMessage private constructor(val text: String) {
    companion object {
        const val MAX_LENGTH = 200

        fun create(rawText: String): Result<SellerMessage> {
            val text = rawText.trim()
            return when {
                text.isBlank() -> Result.Error(SellerMessageFailure.Blank())
                text.length > MAX_LENGTH -> Result.Error(SellerMessageFailure.TooLong())
                else -> Result.Success(SellerMessage(text))
            }
        }
    }
}

sealed class SellerMessageFailure : Exception() {
    class Blank : SellerMessageFailure()
    class TooLong : SellerMessageFailure()
}
