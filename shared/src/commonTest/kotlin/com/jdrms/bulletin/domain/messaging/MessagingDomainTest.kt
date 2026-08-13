package com.jdrms.bulletin.domain.messaging

import com.jdrms.bulletin.domain.messaging.domain.service.MessagingPolicy
import kotlin.test.Test
import kotlin.test.assertTrue

class MessagingDomainTest {

    private val policy = MessagingPolicy()

    @Test
    fun testValidMessageContentPasses() {
        val result = policy.validateMessageContent("Hello, is this item available?")
        assertTrue(result.isSuccess())
    }

    @Test
    fun testBlankMessageContentFails() {
        val result = policy.validateMessageContent("   ")
        assertTrue(result.isError())
    }

    @Test
    fun testBlankReportReasonFails() {
        val result = policy.validateReportReason("")
        assertTrue(result.isError())
    }
}
