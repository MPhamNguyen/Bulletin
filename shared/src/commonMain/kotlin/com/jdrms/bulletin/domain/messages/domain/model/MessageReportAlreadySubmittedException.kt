package com.jdrms.bulletin.domain.messages.domain.model

class MessageReportAlreadySubmittedException : IllegalStateException(
    "You already have an open report for this message."
)
