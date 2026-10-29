package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.app.integration.ProfileConversationUserBlocker
import com.jdrms.bulletin.app.integration.ProfileMarketplaceBlockedUserProvider
import com.jdrms.bulletin.app.integration.ProfileMarketplaceUserBlocker
import com.jdrms.bulletin.app.integration.ProfileMarketplaceUserReporter
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.domain.profile.application.BlockUser
import com.jdrms.bulletin.domain.profile.application.GetBlockedUsers
import com.jdrms.bulletin.domain.profile.application.PublishStudentReview
import com.jdrms.bulletin.domain.profile.application.RegisterStudent
import com.jdrms.bulletin.domain.profile.application.RequestPasswordReset
import com.jdrms.bulletin.domain.profile.application.ResendVerificationCode
import com.jdrms.bulletin.domain.profile.application.RestoreAuthenticatedProfile
import com.jdrms.bulletin.domain.profile.application.SignInExternally
import com.jdrms.bulletin.domain.profile.application.SignInUser
import com.jdrms.bulletin.domain.profile.application.SignOutUser
import com.jdrms.bulletin.domain.profile.application.SoftDeleteProfile
import com.jdrms.bulletin.domain.profile.application.SubmitStudentReview
import com.jdrms.bulletin.domain.profile.application.SubmitUserReport
import com.jdrms.bulletin.domain.profile.application.UnblockUser
import com.jdrms.bulletin.domain.profile.application.UpdatePassword
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.UploadProfilePhoto
import com.jdrms.bulletin.domain.profile.application.VerifyPasswordResetCode
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfilePhotoRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import com.jdrms.bulletin.domain.profile.domain.repository.UserBlockRepository
import com.jdrms.bulletin.domain.profile.domain.repository.UserReportRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.AuthSessionRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.ExternalAuthBrowser
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfilePhotoRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryUserBlockRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryUserReportRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.ProfileReadCache
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseProfilePhotoRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseUserBlockRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseUserReportRepository

internal class ProfileModule(
    private val infrastructure: InfrastructureModule,
    private val externalAuthBrowser: ExternalAuthBrowser? = null
) {
    private val profileReadCache by lazy { ProfileReadCache() }
    internal val profileRepository: ProfileRepository by lazy {
        infrastructure.backend(
            supabase = { SupabaseProfileRepository(it, readCache = profileReadCache) },
            inMemory = { InMemoryProfileRepository() }
        )
    }
    private val userReportRepository: UserReportRepository by lazy {
        infrastructure.backend(
            supabase = ::SupabaseUserReportRepository,
            inMemory = ::InMemoryUserReportRepository
        )
    }
    private val userBlockRepository: UserBlockRepository by lazy {
        infrastructure.backend(
            supabase = ::SupabaseUserBlockRepository,
            inMemory = ::InMemoryUserBlockRepository
        )
    }
    internal val profilePhotoRepository: ProfilePhotoRepository by lazy {
        infrastructure.backend(
            supabase = ::SupabaseProfilePhotoRepository,
            inMemory = ::InMemoryProfilePhotoRepository
        )
    }
    internal val authRepository: AuthRepository by lazy {
        infrastructure.backend(
            supabase = { client ->
                SupabaseAuthRepository(
                    client,
                    profileRepository,
                    externalAuthBrowser
                ) { profileReadCache.clearCurrentSession() }
            },
            inMemory = { InMemoryAuthRepository(profileRepository) }
        )
    }
    val sessionRepository by lazy { AuthSessionRepository(authRepository) }
    private val submitUserReport by lazy { SubmitUserReport(authRepository, userReportRepository) }
    private val blockUser by lazy { BlockUser(authRepository, userBlockRepository) }
    val marketplaceBlockedUserProvider by lazy { ProfileMarketplaceBlockedUserProvider(userBlockRepository) }
    val marketplaceUserReporter by lazy { ProfileMarketplaceUserReporter(submitUserReport) }
    val marketplaceUserBlocker by lazy { ProfileMarketplaceUserBlocker(blockUser) }
    internal val unblockUser by lazy { UnblockUser(authRepository, userBlockRepository) }
    internal val getBlockedUsers by lazy { GetBlockedUsers(authRepository, userBlockRepository, profileRepository) }
    fun conversationUserBlocker(signal: RefreshSignal) =
        ProfileConversationUserBlocker(authRepository, userBlockRepository, blockUser, unblockUser, signal)

    val signInExternally by lazy { SignInExternally(authRepository) }
    val signInUser by lazy { SignInUser(authRepository) }
    val registerStudent by lazy { RegisterStudent(authRepository) }
    val restoreAuthenticatedProfile by lazy { RestoreAuthenticatedProfile(authRepository) }
    val signOutUser by lazy { SignOutUser(authRepository) }
    val resendVerificationCode by lazy { ResendVerificationCode(authRepository) }
    val verifyStudentEmail by lazy { VerifyStudentEmail(authRepository) }
    val requestPasswordReset by lazy { RequestPasswordReset(authRepository) }
    val verifyPasswordResetCode by lazy { VerifyPasswordResetCode(authRepository) }
    val updatePassword by lazy { UpdatePassword(authRepository) }
    val updateStudentProfile by lazy { UpdateStudentProfile(profileRepository) }
    val uploadProfilePhoto by lazy { UploadProfilePhoto(profilePhotoRepository, profileRepository) }
    val softDeleteProfile by lazy { SoftDeleteProfile(profileRepository, signOutUser) }
    val submitStudentReview by lazy { SubmitStudentReview(profileRepository) }
    val publishStudentReview by lazy { PublishStudentReview(submitStudentReview) }
}
