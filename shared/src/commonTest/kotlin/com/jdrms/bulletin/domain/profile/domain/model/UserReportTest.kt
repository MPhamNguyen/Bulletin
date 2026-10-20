package com.jdrms.bulletin.domain.profile.domain.model

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.reporting.domain.model.ReportReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class UserReportTest {
    @Test
    fun directConstructionCannotBypassSelfReportRule() {
        assertFailsWith<IllegalArgumentException> {
            UserReport(UserId("same"), UserId("same"), ReportReason.SCAM_OR_FRAUD, "")
        }
    }

    @Test
    fun copyCannotBypassSelfReportOrDescriptionRules() {
        val report = validReport()

        assertFailsWith<IllegalArgumentException> { report.copy(reportedUserId = report.reporterId) }
        assertFailsWith<IllegalArgumentException> {
            report.copy(description = "x".repeat(UserReport.MAX_DESCRIPTION_LENGTH + 1))
        }
        assertFailsWith<IllegalArgumentException> { report.copy(description = " untrimmed ") }
    }

    @Test
    fun directConstructionAcceptsEmptyAndMaximumLengthDescriptions() {
        listOf("", "x".repeat(UserReport.MAX_DESCRIPTION_LENGTH)).forEach { description ->
            assertEquals(description, validReport().copy(description = description).description)
        }
    }

    @Test
    fun missingReporterOrTargetFailsBeforePersistence() {
        listOf(UserId("") to UserId("target"), UserId("reporter") to UserId(" ")).forEach { (reporter, target) ->
            assertIs<Result.Error>(UserReport.create(reporter, target, ReportReason.SCAM_OR_FRAUD, ""))
            assertFailsWith<IllegalArgumentException> {
                UserReport(reporter, target, ReportReason.SCAM_OR_FRAUD, "")
            }
        }
        assertFailsWith<IllegalArgumentException> { validReport().copy(reportedUserId = UserId("")) }
    }

    private fun validReport() = UserReport.create(
        UserId("reporter"),
        UserId("target"),
        ReportReason.SCAM_OR_FRAUD,
        "Details"
    ).getOrThrow()
}
