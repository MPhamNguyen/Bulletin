package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class EmailVerificationCodeTest {
    @Test
    fun acceptsSixDigitsAndPreservesLeadingZeros() {
        val code = assertIs<Result.Success<EmailVerificationCode>>(EmailVerificationCode.parse(" 012345 ")).data
        assertEquals("012345", code.value)
        assertFalse(code.toString().contains(code.value))
    }

    @Test
    fun rejectsMissingShortLongAndNonAsciiCodes() {
        listOf("", "12345", "1234567", "abcdef", "123 45", "１２３４５６").forEach {
            assertIs<Result.Error>(EmailVerificationCode.parse(it))
        }
    }
}
