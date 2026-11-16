package com.jdrms.bulletin.domain.profile.domain.model

class SoftDeletedAccountException : IllegalStateException(MESSAGE) {
    companion object {
        const val MESSAGE = "This account was deleted. Restore it to continue."
    }
}
