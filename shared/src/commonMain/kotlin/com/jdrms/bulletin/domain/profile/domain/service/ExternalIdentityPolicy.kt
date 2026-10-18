package com.jdrms.bulletin.domain.profile.domain.service

import com.jdrms.bulletin.domain.profile.domain.model.ExternalSignInFailure
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail

data class ExternalIdentityEvidence(
    val email: String?,
    val identityEmail: String?,
    val verified: Boolean,
    val confirmed: Boolean,
    val hasOtherSignInMethod: Boolean
)

/** MVP accepts any verified external email; school membership is not inferred. */
object ExternalIdentityPolicy {
    fun rejection(evidence: ExternalIdentityEvidence): ExternalSignInFailure? = when {
        evidence.email.isNullOrBlank() -> ExternalSignInFailure.MissingEmail
        evidence.hasOtherSignInMethod -> ExternalSignInFailure.AccountConflict
        !StudentEmail.isValid(evidence.email) -> ExternalSignInFailure.MissingEmail
        !evidence.verified || !evidence.confirmed ||
            !evidence.email.trim().equals(evidence.identityEmail?.trim(), ignoreCase = true) ->
            ExternalSignInFailure.UnverifiedEmail
        else -> null
    }
}
