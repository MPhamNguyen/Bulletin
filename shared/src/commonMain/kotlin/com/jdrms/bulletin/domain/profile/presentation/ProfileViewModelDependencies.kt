package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.ManageProfile
import com.jdrms.bulletin.domain.profile.application.ProfileActiveListingsProvider
import com.jdrms.bulletin.domain.profile.application.ResendVerificationCode
import com.jdrms.bulletin.domain.profile.application.RestoreAuthenticatedProfile
import com.jdrms.bulletin.domain.profile.application.SignOutUser
import com.jdrms.bulletin.domain.profile.application.SoftDeleteProfile
import com.jdrms.bulletin.domain.profile.application.SubmitStudentReview
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.UploadProfilePhoto
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy

data class ProfileViewModelDependencies(
    val authenticateUser: AuthenticateUser,
    val restoreAuthenticatedProfile: RestoreAuthenticatedProfile,
    val signOutUser: SignOutUser,
    val verifyStudentEmail: VerifyStudentEmail,
    val resendVerificationCode: ResendVerificationCode,
    val manageProfile: ManageProfile,
    val updateStudentProfile: UpdateStudentProfile,
    val submitStudentReview: SubmitStudentReview,
    val uploadProfilePhoto: UploadProfilePhoto? = null,
    val policy: ProfileValidationPolicy = ProfileValidationPolicy(),
    val defaultUserId: UserId = UserId("current_student"),
    val activeListingsProvider: ProfileActiveListingsProvider? = null,
    val listingChangedSignal: RefreshSignal? = null,
    val softDeleteProfile: SoftDeleteProfile
)
