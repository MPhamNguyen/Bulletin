package com.jdrms.bulletin.domain.home.domain.model

import com.jdrms.bulletin.core.common.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SellerMessageTest {
    @Test
    fun rejectsEmptyAndWhitespaceOnlyText() {
        listOf("", " ", "\t\n\r ").forEach { text ->
            assertIs<SellerMessageFailure.Blank>(assertIs<Result.Error>(SellerMessage.create(text)).exception)
        }
    }

    @Test
    fun storesTrimmedTextAndPreservesInternalNewlines() {
        assertEquals("hello\nthere", SellerMessage.create(" \t hello\nthere \r\n").getOrThrow().text)
    }

    @Test
    fun accepts199And200UnitsAndRejects201() {
        listOf(199, 200).forEach { length ->
            val text = "a".repeat(length)
            assertEquals(text, SellerMessage.create(text).getOrThrow().text)
        }
        assertIs<SellerMessageFailure.TooLong>(
            assertIs<Result.Error>(SellerMessage.create("a".repeat(201))).exception
        )
    }

    @Test
    fun measuresLengthAfterTrimming() {
        val text = "a".repeat(200)
        assertEquals(text, SellerMessage.create(" \n$text\t ").getOrThrow().text)
    }

    @Test
    fun emojiConsumesTwoUtf16UnitsAtBoundary() {
        val accepted = "a".repeat(198) + "\uD83D\uDE00"
        val rejected = "a".repeat(199) + "\uD83D\uDE00"

        assertEquals(200, accepted.length)
        assertEquals(accepted, SellerMessage.create(accepted).getOrThrow().text)
        assertIs<SellerMessageFailure.TooLong>(assertIs<Result.Error>(SellerMessage.create(rejected)).exception)
    }

    @Test
    fun equalTrimmedMessagesHaveValueEquality() {
        assertEquals(SellerMessage.create(" hello ").getOrThrow(), SellerMessage.create("hello").getOrThrow())
    }
}
