package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.domain.profile.domain.model.ExternalSignInOutcome
import com.jdrms.bulletin.domain.profile.domain.repository.ExternalSignInRepository

class SignInExternally(private val authRepository: ExternalSignInRepository) {
    suspend operator fun invoke(): ExternalSignInOutcome = authRepository.signInExternally()
}
