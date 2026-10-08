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
        profiles[profile.id.value] = profile
        return Result.Success(profile)
    }

    override suspend fun softDelete(userId: UserId, deletedAt: String): Result<Unit> {
        val existing = profiles[userId.value] ?: return Result.Error(IllegalArgumentException("Profile not found."))
        profiles[userId.value] = existing.copy(deletedAt = deletedAt)
        return Result.Success(Unit)
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
            ),
            "seller_101" to ProfileDto(
                id = "seller_101",
                email = "dominic.alfonso@student.csulb.edu",
                fullName = "Dominic Alfonso",
                major = "Computer Science",
                university = "CSU Long Beach",
                bio = "Senior Computer Science student @ CSULB. Buying and selling tech & textbooks.",
                isVerified = true
            ),
            "seller_102" to ProfileDto(
                id = "seller_102",
                email = "sean.gallagher@student.csulb.edu",
                fullName = "Sean Gallagher",
                major = "Business Administration",
                university = "CSU Long Beach",
                bio = "Junior in Business Administration. Audiophile and tech enthusiast.",
                isVerified = true
            ),
            "seller_103" to ProfileDto(
                id = "seller_103",
                email = "jacob.ayoub@student.csulb.edu",
                fullName = "Jacob Ayoub",
                major = "Mechanical Engineering",
                university = "CSU Long Beach",
                bio = "Sophomore MechE. Moving off-campus, clearing dorm furniture.",
                isVerified = true
            ),
            "seller_104" to ProfileDto(
                id = "seller_104",
                email = "roger.carrillo@student.csulb.edu",
                fullName = "Roger Carrillo",
                major = "Electrical Engineering",
                university = "CSU Long Beach",
                bio = "EE Senior. Go Beach!",
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
            ),
            "seller_101" to listOf(
                ReviewDto(
                    id = "rev_101_1",
                    reviewerId = "seller_102",
                    reviewerName = "Sean G.",
                    revieweeId = "seller_101",
                    score = 5,
                    comment = "Super fast meetup at the library, textbook was in pristine shape!",
                    createdAt = "1970-01-01T00:00:01Z"
                ),
                ReviewDto(
                    id = "rev_101_2",
                    reviewerId = "seller_103",
                    reviewerName = "Jacob A.",
                    revieweeId = "seller_101",
                    score = 5,
                    comment = "Great seller, communicative and punctual. Highly recommend.",
                    createdAt = "1970-01-01T00:00:02Z"
                )
            ),
            "seller_102" to listOf(
                ReviewDto(
                    id = "rev_102_1",
                    reviewerId = "seller_101",
                    reviewerName = "Dominic A.",
                    revieweeId = "seller_102",
                    score = 5,
                    comment = "Headphones were practically brand new. Smooth trade!",
                    createdAt = "1970-01-01T00:00:01Z"
                )
            ),
            "seller_103" to listOf(
                ReviewDto(
                    id = "rev_103_1",
                    reviewerId = "seller_104",
                    reviewerName = "Roger C.",
                    revieweeId = "seller_103",
                    score = 5,
                    comment = "Helped carry the fridge down to the curb. Awesome guy!",
                    createdAt = "1970-01-01T00:00:01Z"
                )
            ),
            "seller_104" to listOf(
                ReviewDto(
                    id = "rev_104_1",
                    reviewerId = "seller_101",
                    reviewerName = "Dominic A.",
                    revieweeId = "seller_104",
                    score = 5,
                    comment = "Crewneck looks great. Super polite and responsive.",
                    createdAt = "1970-01-01T00:00:01Z"
                )
            )
        )
    }
}

class InMemoryAuthRepository(
    private val profileRepository: ProfileRepository = InMemoryProfileRepository(),
    initialCredentials: Map<String, String> = defaultSeedCredentials,
    initialSeedUserIds: Map<String, UserId> = defaultSeedUserIds,
    // Deterministic test seam only: no email delivery, expiry, or rate-limit simulation.
    private val testVerificationCode: String? = null
) : AuthRepository {

    // Development/test adapter only. Production authentication must never retain raw passwords in application memory.
    private val credentials = initialCredentials.mapKeys { it.key.lowercase() }.toMutableMap()
    private val seedUserIds = initialSeedUserIds.mapKeys { it.key.lowercase() }
    private val profilesByEmail = mutableMapOf<String, StudentProfile>()
    private var currentUser: StudentProfile? = null
    private var pendingPasswordResetEmail: String? = null
    private var passwordResetCodeVerified = false
    private val pendingEmails = mutableSetOf<String>()

    override suspend fun getCurrentUserId(): Result<UserId?> = Result.Success(currentUser?.id)

    override suspend fun getCurrentUser(): Result<StudentProfile?> = Result.Success(currentUser)

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

        val userProfile = profilesByEmail[normalizedEmail]
            ?: seedUserIds[normalizedEmail]?.let { userId ->
                when (val result = profileRepository.getProfile(userId)) {
                    is Result.Success -> result.data?.takeIf { it.email == email }
                    is Result.Error -> null
                }
            }
        if (userProfile != null) {
            if (userProfile.isDeleted) {
                return Result.Error(
                    IllegalArgumentException("Account not found. Please check your email or create an account.")
                )
            }
            currentUser = userProfile
            return Result.Success(userProfile)
        }

        val error = IllegalArgumentException("Account not found. Please check your email or create an account.")
        return Result.Error(error)
    }

    override suspend fun requestPasswordReset(email: StudentEmail): Result<Unit> {
        if (testVerificationCode == null) {
            return Result.Error(IllegalStateException("Password reset requires a configured authentication service."))
        }
        val normalizedEmail = email.value.lowercase()
        if (!credentials.containsKey(normalizedEmail)) {
            return Result.Error(IllegalArgumentException("No account exists for that email address."))
        }
        pendingPasswordResetEmail = normalizedEmail
        passwordResetCodeVerified = false
        return Result.Success(Unit)
    }

    override suspend fun verifyPasswordResetCode(email: StudentEmail, code: String): Result<Unit> {
        if (pendingPasswordResetEmail != email.value.lowercase()) {
            return Result.Error(IllegalStateException("Request a new password reset before entering a code."))
        }
        if (code.trim() != testVerificationCode) {
            return Result.Error(IllegalArgumentException("The confirmation code is incorrect."))
        }
        passwordResetCodeVerified = true
        return Result.Success(Unit)
    }

    override suspend fun updatePassword(password: String): Result<Unit> {
        val email = pendingPasswordResetEmail
            ?: return Result.Error(IllegalStateException("Start a password reset before choosing a new password."))
        if (!passwordResetCodeVerified) {
            return Result.Error(IllegalStateException("Verify the confirmation code before choosing a new password."))
        }
        credentials[email] = password
        pendingPasswordResetEmail = null
        passwordResetCodeVerified = false
        return Result.Success(Unit)
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
        if (credentials.containsKey(normalizedEmail)) {
            return Result.Error(IllegalArgumentException("An account with this email already exists."))
        }

        val generatedId = "user_${generateUuid().take(8)}"
        val newProfile = StudentProfile(
            id = UserId(generatedId),
            email = email,
            fullName = fullName,
            university = university,
            isVerified = false
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
        val defaultSeedUserIds = mapOf(
            "dominic.alfonso@student.csulb.edu" to UserId("current_student")
        )
    }
}
