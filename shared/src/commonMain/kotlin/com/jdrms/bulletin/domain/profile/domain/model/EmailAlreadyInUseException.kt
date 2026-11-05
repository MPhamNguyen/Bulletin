package com.jdrms.bulletin.domain.profile.domain.model

class EmailAlreadyInUseException : IllegalStateException(MESSAGE) {
    companion object {
        const val MESSAGE = "An account with this email address already exists. Please log in instead."
    }
}
