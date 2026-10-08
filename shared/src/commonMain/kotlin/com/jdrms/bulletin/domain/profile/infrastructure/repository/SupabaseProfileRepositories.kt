package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ProfileDto
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewDto
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewInsertDto
import com.jdrms.bulletin.domain.profile.infrastructure.mapper.ProfileMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseProfileRepository(
    private val supabase: SupabaseClient,
    private val policy: ProfileValidationPolicy = ProfileValidationPolicy()
) : ProfileRepository {

    override suspend fun getProfile(userId: UserId): Result<StudentProfile?> {
        return runCatching {
            val resolvedId = resolveUserId(userId) ?: return@runCatching null

            val dto = supabase.from(PROFILES_TABLE).select {
                filter {
                    eq("id", resolvedId)
                }
            }.decodeSingleOrNull<ProfileDto>()

            dto?.let {
                val reputation = getReputation(UserId(resolvedId))
                ProfileMapper.toDomain(it, reputation)
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapProfileErrorMessage(it), it)) }
        )
    }

    override suspend fun updateProfile(profile: StudentProfile): Result<StudentProfile> {
        return runCatching {
            val resolvedId = resolveUserId(profile.id) ?: profile.id.value
            val profileToSave = if (resolvedId != profile.id.value) {
                profile.copy(id = UserId(resolvedId))
            } else {
                profile
            }
            val updateDto = ProfileMapper.toUpdateDto(profileToSave)
            supabase.from(PROFILES_TABLE).update(updateDto) {
                filter {
                    eq("id", resolvedId)
                }
            }
            profileToSave
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapProfileErrorMessage(it), it)) }
        )
    }

    override suspend fun softDelete(userId: UserId, deletedAt: String): Result<Unit> {
        return runCatching {
            val resolvedId = resolveUserId(userId) ?: userId.value
            supabase.from(PROFILES_TABLE).update(
                buildJsonObject {
                    put("deleted_at", JsonPrimitive(deletedAt))
                }
            ) {
                filter {
                    eq("id", resolvedId)
                }
            }
            Unit
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapProfileErrorMessage(it), it)) }
        )
    }

    override suspend fun submitReview(targetUserId: UserId, review: StudentReview): Result<Unit> {
        return runCatching {
            val currentUserId = supabase.auth.currentUserOrNull()?.id
                ?: error("You must be logged in to submit a review.")
            val resolvedTargetId = resolveUserId(targetUserId) ?: targetUserId.value

            val insertDto = ReviewInsertDto(
                reviewerId = currentUserId,
                revieweeId = resolvedTargetId,
                score = review.rating.score,
                comment = review.comment
            )
            supabase.from(REVIEWS_TABLE).insert(insertDto)
            Unit
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapProfileErrorMessage(it), it)) }
        )
    }

    override suspend fun getReputation(userId: UserId): StudentReputation {
        val resolvedId = resolveUserId(userId) ?: return policy.calculateReputation(userId, emptyList())

        val reviews = runCatching {
            val dtos = runCatching {
                supabase.from(REVIEWS_VIEW).select {
                    filter {
                        eq("reviewee_id", resolvedId)
                    }
                }.decodeList<ReviewDto>()
            }.getOrElse {
                supabase.from(REVIEWS_TABLE).select {
                    filter {
                        eq("reviewee_id", resolvedId)
                    }
                }.decodeList<ReviewDto>()
            }
            dtos.map { ProfileMapper.toDomain(it) }
        }.getOrDefault(emptyList())

        return policy.calculateReputation(UserId(resolvedId), reviews)
    }

    private fun resolveUserId(userId: UserId): String? {
        return if (userId.value == "current_student") {
            supabase.auth.currentUserOrNull()?.id
        } else if (isValidUuid(userId.value)) {
            userId.value
        } else {
            null
        }
    }

    companion object {
        const val PROFILES_TABLE = "profiles"
        const val REVIEWS_TABLE = "reviews"
        const val REVIEWS_VIEW = "reviews_with_names"

        private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

        fun isValidUuid(value: String): Boolean = UUID_REGEX.matches(value)

        private val PROFILE_ERROR_RULES = listOf(
            listOf("could not find the table", "schema cache") to
                "Database table not found. Please verify your Supabase schema setup.",
            listOf("unable to resolve host", "failed to connect", "timeout", "request timeout") to
                "Unable to connect to server. Please check your internet connection.",
            listOf("jwt", "unauthorized", "invalid api key", "no api key") to
                "Unauthorized database request. Please check your Supabase API credentials.",
            listOf("row-level security", "rls") to
                "Database permission denied. Please check your Supabase RLS policies.",
            listOf("invalid input syntax for type uuid", "22p02") to
                "Invalid user identifier format."
        )

        fun mapProfileErrorMessage(throwable: Throwable): String {
            val message = throwable.message ?: return "An unexpected profile error occurred."
            val lower = message.lowercase()

            for ((patterns, mappedMessage) in PROFILE_ERROR_RULES) {
                if (patterns.any { lower.contains(it) }) {
                    return mappedMessage
                }
            }

            val firstLine = message.lines().firstOrNull { it.isNotBlank() }?.trim() ?: "Profile request failed."
            val isTechnicalDump = firstLine.startsWith("url:", ignoreCase = true) ||
                firstLine.startsWith("headers:", ignoreCase = true) ||
                firstLine.startsWith("http method:", ignoreCase = true)

            return if (isTechnicalDump) {
                "Profile request failed. Please check your connection and try again."
            } else {
                firstLine
            }
        }
    }
}
