package com.jdrms.bulletin.domain.profile.domain.model

sealed interface ExternalSignInOutcome {
    data class SignedIn(val profile: StudentProfile) : ExternalSignInOutcome
    data object Cancelled : ExternalSignInOutcome
    data class Failed(val reason: ExternalSignInFailure) : ExternalSignInOutcome
}

enum class ExternalSignInFailure {
    Unavailable, Ineligible, UnverifiedEmail, MissingEmail, AccountConflict,
    Network, InvalidCallback, ProfileUnavailable, Technical
}
