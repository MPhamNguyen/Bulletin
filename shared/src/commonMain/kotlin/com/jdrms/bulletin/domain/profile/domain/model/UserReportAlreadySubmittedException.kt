package com.jdrms.bulletin.domain.profile.domain.model

class UserReportAlreadySubmittedException :
    IllegalStateException("You have already reported this user.")
