package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.domain.profile.domain.model.ExternalSignInFailure
import com.jdrms.bulletin.domain.profile.domain.service.ExternalIdentityEvidence
import com.jdrms.bulletin.domain.profile.domain.service.ExternalIdentityPolicy
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** Translates server-owned identity data; user_metadata is never verification evidence. */
internal object MicrosoftIdentityPolicy {
    fun rejection(user: UserInfo): ExternalSignInFailure? {
        val identities = user.identities.orEmpty()
        val microsoft = identities.singleOrNull { it.provider == "azure" }
            ?: return ExternalSignInFailure.Ineligible
        return ExternalIdentityPolicy.rejection(
            ExternalIdentityEvidence(
                email = user.email,
                identityEmail = (microsoft.identityData["email"] as? JsonPrimitive)?.content,
                verified = (microsoft.identityData["email_verified"] as? JsonPrimitive)?.booleanOrNull == true,
                confirmed = user.emailConfirmedAt != null,
                hasOtherSignInMethod = identities.any { it.provider != "azure" }
            )
        )
    }
}
