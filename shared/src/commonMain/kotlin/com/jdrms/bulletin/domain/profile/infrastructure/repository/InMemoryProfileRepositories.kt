package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.generateUuid
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
                    createdAtMillis = 1000L
                ),
                ReviewDto(
                    id = "rev_2",
                    reviewerId = "user_202",
                    reviewerName = "Jacob A.",
                    revieweeId = "current_student",
                    score = 5,
                    comment = "Great buyer, communicative and punctual. Highly recommend.",
                    createdAtMillis = 2000L
                )
            )
        )
    }
}

class InMemoryAuthRepository(
    private val profileRepository: ProfileRepository = InMemoryProfileRepository(),
    initialCredentials: Map<String, String> = defaultSeedCredentials
) : AuthRepository {

    // Development/test adapter only. Production authentication must never retain raw passwords in application memory.
    private val credentials = initialCredentials.mapKeys { it.key.lowercase() }.toMutableMap()
    private val profilesByEmail = mutableMapOf<String, StudentProfile>()
    private var currentUser: StudentProfile? = null

    override suspend fun getCurrentUser(): Result<StudentProfile?> = Result.Success(currentUser)

    override suspend fun login(email: StudentEmail, password: String): Result<StudentProfile> {
        val normalizedEmail = email.value.lowercase()
        val storedPassword = credentials[normalizedEmail]

        if (storedPassword == null) {
            val error = IllegalArgumentException("Account not found. Please check your email or create an account.")
            return Result.Error(error)
        }

        if (storedPassword != password) {
            return Result.Error(IllegalArgumentException("Incorrect password. Please try again."))
        }

        val userProfile = profilesByEmail[normalizedEmail]
            ?: when (val res = profileRepository.getProfile(UserId("current_student"))) {
                is Result.Success -> res.data
                is Result.Error -> null
            }
        if (userProfile != null) {
            currentUser = userProfile
            return Result.Success(userProfile)
        }

        val error = IllegalArgumentException("Account not found. Please check your email or create an account.")
        return Result.Error(error)
    }

    override suspend fun register(
        email: StudentEmail,
        password: String,
        fullName: String,
        university: String
    ): Result<StudentProfile> {
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
        profileRepository.updateProfile(newProfile)
        currentUser = newProfile
        return Result.Success(newProfile)
    }

    override suspend fun verifyEmail(email: StudentEmail, code: String): Result<Boolean> {
        return Result.Success(code.trim().isNotEmpty())
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
