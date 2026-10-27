package com.jdrms.bulletin.domain.reporting.domain.model

enum class ReportType {
    MESSAGE,
    USER,
    LISTING
}

enum class ReportStatus(val isOpen: Boolean) {
    OPEN(true),
    UNDER_REVIEW(true),
    ACTION_TAKEN(false),
    DISMISSED(false),
    DUPLICATE(false),
    UNKNOWN(true);

    companion object {
        fun fromDatabaseValue(value: String?): ReportStatus = when (value?.lowercase()) {
            "open" -> OPEN
            "under_review", "in_review" -> UNDER_REVIEW
            "action_taken", "closed", "resolved" -> ACTION_TAKEN
            "dismissed" -> DISMISSED
            "duplicate" -> DUPLICATE
            else -> UNKNOWN
        }
    }
}

/** [code] is the stable database value; [label] is presentation text and may change. */
enum class ReportReason(val code: String, val label: String, val reportTypes: Set<ReportType>) {
    INAPPROPRIATE_CONTENT("inappropriate_content", "Inappropriate Content", ReportType.entries.toSet()),
    SCAM_OR_FRAUD("scam_or_fraud", "Scam or Fraud", ReportType.entries.toSet()),
    HARASSMENT_OR_HATE("harassment_or_hate", "Harassment or Hate", setOf(ReportType.MESSAGE, ReportType.USER)),
    THREATS_OR_VIOLENCE("threats_or_violence", "Threats or Violence", setOf(ReportType.MESSAGE)),
    SPAM("spam", "Spam", setOf(ReportType.MESSAGE)),
    SHARING_PRIVATE_INFO("sharing_private_info", "Sharing Private Info", setOf(ReportType.MESSAGE)),
    FAKE_ACCOUNT_OR_IMPERSONATION(
        "fake_account_or_impersonation",
        "Fake Account / Impersonation",
        setOf(ReportType.USER)
    ),
    PROHIBITED_OR_ILLEGAL_ITEM(
        "prohibited_or_illegal_item",
        "Prohibited or Illegal Item",
        setOf(ReportType.LISTING)
    ),
    MISLEADING_ITEM("misleading_item", "Misleading Item", setOf(ReportType.LISTING)),
    COUNTERFEIT_OR_STOLEN_GOODS(
        "counterfeit_or_stolen_goods",
        "Counterfeit or Stolen Goods",
        setOf(ReportType.LISTING)
    ),
    UNSAFE_MEETUP_OR_TRANSACTION(
        "unsafe_meetup_or_transaction",
        "Unsafe Meetup or Transaction",
        setOf(ReportType.LISTING)
    ),
    OTHER("other", "Other", ReportType.entries.toSet());

    fun appliesTo(type: ReportType): Boolean = type in reportTypes

    companion object {
        fun forType(type: ReportType): List<ReportReason> = entries.filter { it.appliesTo(type) }
    }
}
