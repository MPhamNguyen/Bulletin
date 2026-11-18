package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.StudentReviewPage
import com.jdrms.bulletin.domain.profile.infrastructure.dto.PublicProfileIdentityDto
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewDto
import com.jdrms.bulletin.domain.profile.infrastructure.mapper.ReviewMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

internal class SupabaseProfileReviewReader(private val supabase: SupabaseClient) {
    suspend fun getReviews(revieweeId: String): List<StudentReview> {
        val rows = supabase.from(SupabaseProfileRepository.REVIEWS_TABLE).select {
            filter { eq("reviewee_id", revieweeId) }
        }.decodeList<ReviewDto>()
        return resolveReviewers(rows)
    }

    suspend fun getReviewsPage(revieweeId: String, offset: Int, pageSize: Int): StudentReviewPage {
        val rows = supabase.from(SupabaseProfileRepository.REVIEWS_TABLE).select {
            filter { eq("reviewee_id", revieweeId) }
            order("created_at", Order.DESCENDING)
            order("id", Order.DESCENDING)
            range(offset.toLong(), (offset + pageSize).toLong())
        }.decodeList<ReviewDto>()
        return StudentReviewPage(
            reviews = resolveReviewers(rows.take(pageSize)),
            nextOffset = if (rows.size > pageSize) offset + pageSize else null
        )
    }

    private suspend fun resolveReviewers(rows: List<ReviewDto>): List<StudentReview> {
        if (rows.isEmpty()) return emptyList()
        val profiles = rows.mapNotNull { it.reviewerId }.distinct().chunked(PROFILE_BATCH_SIZE).flatMap { ids ->
            supabase.from(SupabaseProfileRepository.PROFILES_TABLE).select(Columns.raw("id,full_name")) {
                filter { isIn("id", ids) }
            }.decodeList<PublicProfileIdentityDto>()
        }.associateBy { it.id }
        return rows.map { review ->
            val name = review.reviewerId?.let(profiles::get)?.fullName
            ReviewMapper.toDomain(review.copy(reviewerName = name))
        }
    }

    private companion object {
        const val PROFILE_BATCH_SIZE = 100
    }
}
