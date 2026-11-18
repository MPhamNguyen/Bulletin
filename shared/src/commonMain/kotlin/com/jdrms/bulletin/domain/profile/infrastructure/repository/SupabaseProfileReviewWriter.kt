package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewInsertDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

internal class SupabaseProfileReviewWriter(
    private val supabase: SupabaseClient,
    private val readCache: ProfileReadCache,
    private val resolveUserId: (UserId) -> String?
) {
    suspend fun submit(targetUserId: UserId, review: StudentReview): Result<Unit> {
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
                comment = review.comment,
                listingId = review.relatedListingId
            )
            supabase.from(SupabaseProfileRepository.REVIEWS_TABLE).insert(insertDto)
            Unit
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(ProfileErrorMapping.map(it), it)) }
        )
        if (result is Result.Success) resolvedTargetUserId?.let { readCache.invalidate(it) }
        return result
    }
}
