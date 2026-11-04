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

class SupabaseProfileRepository private constructor(
    private val supabase: SupabaseClient,
    private val policy: ProfileValidationPolicy,
    internal val readCache: ProfileReadCache
) : ProfileRepository {

    constructor(supabase: SupabaseClient) : this(supabase, ProfileValidationPolicy(), ProfileReadCache())

    internal constructor(supabase: SupabaseClient, readCache: ProfileReadCache) :
        this(supabase, ProfileValidationPolicy(), readCache)

    override suspend fun getProfile(userId: UserId): Result<StudentProfile?> {
        return runCatching {
            val currentUserId = supabase.auth.currentUserOrNull()?.id
            readCache.observeSession(currentUserId)
            val resolvedId = resolveUserId(userId) ?: return@runCatching null
            readCache.profile(UserId(resolvedId), isCurrentUser = resolvedId == currentUserId) {
                val dto = supabase.from(PROFILES_TABLE).select {
                    filter { eq("id", resolvedId) }
                }
                    .decodeSingleOrNull<ProfileDto>()
                dto?.let { ProfileMapper.toDomain(it, getReputation(UserId(resolvedId))) }
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapProfileErrorMessage(it), it)) }
        )
    }

    override suspend fun refreshProfile(userId: UserId): Result<StudentProfile?> {
        val resolvedId = resolveUserId(userId) ?: return getProfile(userId)
        readCache.invalidate(UserId(resolvedId))
        return getProfile(UserId(resolvedId))
    }

    override suspend fun updateProfile(profile: StudentProfile): Result<StudentProfile> {
        val result = runCatching {
            val resolvedId = resolveUserId(profile.id) ?: profile.id.value
            val profileToSave = if (resolvedId != profile.id.value) {
                profile.copy(id = UserId(resolvedId))
            } else {
                profile
            }
            val updateDto = ProfileMapper.toUpdateDto(profileToSave)
            val saved = supabase.from(PROFILES_TABLE).update(updateDto) {
                select()
                filter {
                    eq("id", resolvedId)
                }
            }.decodeSingleOrNull<ProfileDto>() ?: error("Profile update did not save any changes. Please try again.")
            ProfileMapper.toDomain(saved, profileToSave.reputation)
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(mapProfileErrorMessage(it), it)) }
        )
        if (result is Result.Success) readCache.invalidate(result.data.id)
        return result
    }

    override suspend fun softDelete(userId: UserId, deletedAt: String): Result<Unit> {
        var resolvedUserId: UserId? = null
        val result = runCatching {
            val resolvedId = resolveUserId(userId) ?: userId.value
            resolvedUserId = UserId(resolvedId)
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
        if (result is Result.Success) resolvedUserId?.let { readCache.invalidate(it) }
        return result
    }

    override suspend fun submitReview(targetUserId: UserId, review: StudentReview): Result<Unit> {
        var resolvedTargetUserId: UserId? = null
        val result = runCatching {
            val currentUserId = supabase.auth.currentUserOrNull()?.id
                ?: error("You must be logged in to submit a review.")
            val resolvedTargetId = resolveUserId(targetUserId) ?: targetUserId.value
            resolvedTargetUserId = UserId(resolvedTargetId)

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
        if (result is Result.Success) resolvedTargetUserId?.let { readCache.invalidate(it) }
        return result
    }

    override suspend fun getReputation(userId: UserId): StudentReputation {
        val currentUserId = supabase.auth.currentUserOrNull()?.id
        readCache.observeSession(currentUserId)
        val resolvedId = resolveUserId(userId) ?: return policy.calculateReputation(userId, emptyList())
        return readCache.reputation(UserId(resolvedId), isCurrentUser = resolvedId == currentUserId) {
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
            policy.calculateReputation(UserId(resolvedId), reviews)
        }
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

        fun mapProfileErrorMessage(throwable: Throwable): String = ProfileErrorMapping.map(throwable)
    }
}
