package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileMarketplaceSellerNameProviderTest {
    @Test
    fun sellerVerificationUsesConfirmedStatusRatherThanTheEmailSuffix() = runTest {
        val profiles = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val provider = ProfileMarketplaceSellerNameProvider(profiles)
        for ((email, verified) in listOf("student@school.edu" to false, "student@outlook.com" to true)) {
            val profile = StudentProfile(
                UserId("test-user"),
                StudentEmail(email),
                "Test Student",
                isVerified = verified
            )
            profiles.updateProfile(profile)
            assertEquals(verified, provider.getSellerProfile(profile.id.value)?.isVerified)
        }
    }
}
