package com.jdrms.bulletin.domain.messages.presentation

data class ConversationNavigation(
    val onViewProfile: (String) -> Unit = {},
    val onReportUser: (String) -> Unit = {}
)
