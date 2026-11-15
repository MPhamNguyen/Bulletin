package com.jdrms.bulletin.domain.reporting

import com.jdrms.bulletin.domain.reporting.domain.model.ReportReason
import com.jdrms.bulletin.domain.reporting.domain.model.ReportStatus
import com.jdrms.bulletin.domain.reporting.domain.model.ReportType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportReasonTest {
    @Test
    fun reasonCodesRemainStableAcrossPresentationLabelChanges() {
        assertEquals(
            listOf(
                "inappropriate_content",
                "scam_or_fraud",
                "harassment_or_hate",
                "threats_or_violence",
                "spam",
                "sharing_private_info",
                "fake_account_or_impersonation",
                "prohibited_or_illegal_item",
                "misleading_item",
                "counterfeit_or_stolen_goods",
                "unsafe_meetup_or_transaction",
                "other"
            ),
            ReportReason.entries.map(ReportReason::code)
        )
    }

    @Test
    fun otherAppearsLastForEveryReportType() {
        ReportType.entries.forEach { type ->
            assertEquals(ReportReason.OTHER, ReportReason.forType(type).last())
        }
    }

    @Test
    fun databaseReportStatusesMapToEveryKotlinStatusIncludingUnknownFallback() {
        assertEquals(ReportStatus.OPEN, ReportStatus.fromDatabaseValue("open"))
        assertEquals(ReportStatus.UNDER_REVIEW, ReportStatus.fromDatabaseValue("in_review"))
        assertEquals(ReportStatus.ACTION_TAKEN, ReportStatus.fromDatabaseValue("resolved"))
        assertEquals(ReportStatus.DISMISSED, ReportStatus.fromDatabaseValue("dismissed"))
        assertEquals(ReportStatus.UNKNOWN, ReportStatus.fromDatabaseValue("future_status"))
    }

    @Test
    fun retiredDuplicateValueUsesTheUnknownOpenFallback() {
        val status = ReportStatus.fromDatabaseValue("duplicate")

        assertEquals(ReportStatus.UNKNOWN, status)
        assertTrue(status.isOpen)
    }

    @Test
    fun legacyDatabaseStatusAliasesKeepTheirCanonicalKotlinMeaning() {
        assertEquals(ReportStatus.UNDER_REVIEW, ReportStatus.fromDatabaseValue("under_review"))
        assertEquals(ReportStatus.ACTION_TAKEN, ReportStatus.fromDatabaseValue("action_taken"))
        assertEquals(ReportStatus.ACTION_TAKEN, ReportStatus.fromDatabaseValue("closed"))
    }
}
