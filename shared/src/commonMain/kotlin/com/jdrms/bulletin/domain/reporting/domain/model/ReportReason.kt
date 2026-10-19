package com.jdrms.bulletin.domain.reporting.domain.model

enum class ReportType {
    MESSAGE,
    USER,
    LISTING
}

enum class ReportReason(val label: String, val reportTypes: Set<ReportType>) {
    OTHER("Other", ReportType.entries.toSet()),
    INAPPROPRIATE_CONTENT("Inappropriate Content", ReportType.entries.toSet()),
    SCAM_OR_FRAUD("Scam or Fraud", ReportType.entries.toSet()),
    HARASSMENT_OR_HATE("Harassment or Hate", setOf(ReportType.MESSAGE, ReportType.USER)),
    THREATS_OR_VIOLENCE("Threats or Violence", setOf(ReportType.MESSAGE)),
    SPAM("Spam", setOf(ReportType.MESSAGE)),
    SHARING_PRIVATE_INFO("Sharing Private Info", setOf(ReportType.MESSAGE)),
    FAKE_ACCOUNT_OR_IMPERSONATION("Fake Account / Impersonation", setOf(ReportType.USER)),
    PROHIBITED_OR_ILLEGAL_ITEM("Prohibited or Illegal Item", setOf(ReportType.LISTING)),
    MISLEADING_ITEM("Misleading Item", setOf(ReportType.LISTING)),
    COUNTERFEIT_OR_STOLEN_GOODS("Counterfeit or Stolen Goods", setOf(ReportType.LISTING)),
    UNSAFE_MEETUP_OR_TRANSACTION("Unsafe Meetup or Transaction", setOf(ReportType.LISTING));

    fun appliesTo(type: ReportType): Boolean = type in reportTypes

    companion object {
        fun forType(type: ReportType): List<ReportReason> = entries.filter { it.appliesTo(type) }
    }
}
