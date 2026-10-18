package com.jdrms.bulletin.domain.profile.domain.repository

import com.jdrms.bulletin.domain.profile.domain.model.ExternalSignInFailure
import com.jdrms.bulletin.domain.profile.domain.model.ExternalSignInOutcome

interface ExternalSignInRepository {
    /** External authentication is optional for local development adapters. */
    suspend fun signInExternally(): ExternalSignInOutcome =
        ExternalSignInOutcome.Failed(ExternalSignInFailure.Unavailable)
}
