package com.jdrms.bulletin.domain.reporting

import com.jdrms.bulletin.domain.reporting.domain.model.ReportReason
import com.jdrms.bulletin.domain.reporting.domain.model.ReportType
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
