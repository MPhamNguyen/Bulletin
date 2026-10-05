package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.generateUuid
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationCode
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationOutcome
import com.jdrms.bulletin.domain.profile.domain.model.PendingRegistration
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ProfileDto
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewDto
import com.jdrms.bulletin.domain.profile.infrastructure.mapper.ProfileMapper

class InMemoryProfileRepository(
    private val policy: ProfileValidationPolicy = ProfileValidationPolicy(),
    initialProfiles: Map<String, ProfileDto> = defaultSeedProfiles,
    initialReviews: Map<String, List<ReviewDto>> = defaultSeedReviews
) : ProfileRepository {

    private val profiles = initialProfiles.mapValues { ProfileMapper.toDomain(it.value) }.toMutableMap()
    private val reviewsByUser = initialReviews.mapValues { entry ->
        entry.value.map { ProfileMapper.toDomain(it) }.toMutableList()
    }.toMutableMap()

    override suspend fun getProfile(userId: UserId): Result<StudentProfile?> {
        val baseProfile = profiles[userId.value] ?: return Result.Success(null)
        val rep = getReputation(userId)
        return Result.Success(baseProfile.copy(reputation = rep))
    }

    override suspend fun updateProfile(profile: StudentProfile): Result<StudentProfile> {
        val oldEntry = profiles.entries.find {
            it.value.email.value.equals(profile.email.value, ignoreCase = true) && it.key != profile.id.value
        }
        if (oldEntry != null) {
            profiles.remove(oldEntry.key)
        }
        profiles[profile.id.value] = profile
        return Result.Success(profile)
    }

    override suspend fun submitReview(targetUserId: UserId, review: StudentReview): Result<Unit> {
        val list = reviewsByUser.getOrPut(targetUserId.value) { mutableListOf() }
        list.add(review)
        return Result.Success(Unit)
    }

    override suspend fun getReputation(userId: UserId): StudentReputation {
        val userReviews = reviewsByUser[userId.value] ?: emptyList()
        return policy.calculateReputation(userId, userReviews)
    }

    override suspend fun deleteProfile(userId: UserId, deletedAtMillis: Long): Result<Unit> {
        val existing = profiles[userId.value]
            ?: return Result.Error(NoSuchElementException("Profile not found."))
        return when (val deleted = existing.markDeleted(deletedAtMillis)) {
            is Result.Success -> {
                profiles[userId.value] = deleted.data
                Result.Success(Unit)
            }
            is Result.Error -> deleted
        }
    }

    companion object {
        private val defaultSeedProfiles = mapOf(
            "current_student" to ProfileDto(
                id = "current_student",
                email = "dominic.alfonso@student.csulb.edu",
                fullName = "Dominic Alfonso",
                major = "Computer Science",
                university = "CSU Long Beach",
                bio = "Senior Computer Science student @ CSULB. Buying and selling tech & textbooks.",
                isVerified = true
            )
        )

        private val defaultSeedReviews = mapOf(
            "current_student" to listOf(
                ReviewDto(
                    id = "rev_1",
                    reviewerId = "user_201",
                    reviewerName = "Sean G.",
                    revieweeId = "current_student",
                    score = 5,
                    comment = "Super fast meetup at the campus library, textbook was in pristine shape!",
                    createdAt = "1970-01-01T00:00:01Z"
                ),
                ReviewDto(
                    id = "rev_2",
                    reviewerId = "user_202",
                    reviewerName = "Jacob A.",
                    revieweeId = "current_student",
                    score = 5,
                    comment = "Great buyer, communicative and punctual. Highly recommend.",
                    createdAt = "1970-01-01T00:00:02Z"
                )
            )
        )
    }
}

class InMemoryAuthRepository(
    private val profileRepository: ProfileRepository = InMemoryProfileRepository(),
    initialCredentials: Map<String, String> = defaultSeedCredentials,
    // Deterministic test seam only: no email delivery, expiry, or rate-limit simulation.
    private val testVerificationCode: String? = null
) : AuthRepository {

    // Development/test adapter only. Production authentication must never retain raw passwords in application memory.
    private val credentials = initialCredentials.mapKeys { it.key.lowercase() }.toMutableMap()
    private val profilesByEmail = mutableMapOf<String, StudentProfile>()
    private var currentUser: StudentProfile? = null
    private val pendingEmails = mutableSetOf<String>()

    override suspend fun getCurrentUserId(): Result<UserId?> {
        return Result.Success(currentUser?.takeUnless { it.isDeleted }?.id)
    }

    override suspend fun getCurrentUser(): Result<StudentProfile?> {
        val user = currentUser ?: return Result.Success(null)
        if (user.isDeleted) {
            currentUser = null
            return Result.Success(null)
        }
        return Result.Success(user)
    }

    override suspend fun login(email: StudentEmail, password: String): Result<StudentProfile> {
        val normalizedEmail = email.value.lowercase()
        val storedPassword = credentials[normalizedEmail]
        val loginError = when {
            normalizedEmail in pendingEmails -> "Please verify your email address before logging in."
            storedPassword == null -> "Account not found. Please check your email or create an account."
            storedPassword != password -> "Incorrect password. Please try again."
            else -> null
        }
        if (loginError != null) return Result.Error(IllegalArgumentException(loginError))

        val profileId = profilesByEmail[normalizedEmail]?.id ?: UserId("current_student")
        val userProfile = when (val res = profileRepository.getProfile(profileId)) {
            is Result.Success -> res.data ?: profilesByEmail[normalizedEmail]
            is Result.Error -> profilesByEmail[normalizedEmail]
        }
        return when {
            userProfile == null -> {
                Result.Error(
                    IllegalArgumentException("Account not found. Please check your email or create an account.")
                )
            }
            userProfile.isDeleted -> {
                Result.Error(IllegalArgumentException("Account has been deleted."))
            }
            else -> {
                currentUser = userProfile
                Result.Success(userProfile)
            }
        }
    }

    override suspend fun register(
        email: StudentEmail,
        password: String,
        fullName: String,
        university: String
    ): Result<PendingRegistration> {
        if (testVerificationCode == null) {
            return Result.Error(
                IllegalStateException("Email verification requires a configured authentication service.")
            )
        }
        val normalizedEmail = email.value.lowercase()
        val cached = profilesByEmail[normalizedEmail]
        val existingProfile = if (cached != null) {
            when (val res = profileRepository.getProfile(cached.id)) {
                is Result.Success -> res.data ?: cached
                is Result.Error -> cached
            }
        } else {
            when (val res = profileRepository.getProfile(UserId(normalizedEmail))) {
                is Result.Success -> res.data
                is Result.Error -> null
            }
        }

        if (credentials.containsKey(normalizedEmail) && existingProfile?.isDeleted != true) {
            return Result.Error(IllegalArgumentException("An account with this email already exists."))
        }

        val generatedId = "user_${generateUuid().take(8)}"
        val newProfile = StudentProfile(
            id = UserId(generatedId),
            email = email,
            fullName = fullName,
            university = university,
            isVerified = false,
            deleteAtMillis = null
        )

        credentials[normalizedEmail] = password
        profilesByEmail[normalizedEmail] = newProfile
        pendingEmails.add(normalizedEmail)
        return Result.Success(PendingRegistration(email))
    }

    override suspend fun verifyEmail(
        email: StudentEmail,
        code: EmailVerificationCode
    ): Result<EmailVerificationOutcome> {
        if (email.value !in pendingEmails || code.value != testVerificationCode) {
            return Result.Error(IllegalArgumentException("This code is invalid or has expired."))
        }
        val profile = profilesByEmail.getValue(email.value).confirmEmail()
        profilesByEmail[email.value] = profile
        pendingEmails.remove(email.value)
        currentUser = profile
        return when (val saved = profileRepository.updateProfile(profile)) {
            is Result.Error -> Result.Success(EmailVerificationOutcome.ProfileRecoveryRequired)
            is Result.Success -> {
                currentUser = saved.data
                Result.Success(EmailVerificationOutcome.ProfileAvailable(saved.data))
            }
        }
    }

    override suspend fun resendVerificationCode(email: StudentEmail): Result<Unit> {
        return if (testVerificationCode != null && email.value in pendingEmails) {
            Result.Success(Unit)
        } else {
            Result.Error(IllegalStateException("No email verification is pending."))
        }
    }

    override suspend fun signOut(): Result<Unit> {
        currentUser = null
        return Result.Success(Unit)
    }

    companion object {
        val defaultSeedCredentials = mapOf(
            "dominic.alfonso@student.csulb.edu" to "password123"
        )
    }
}
