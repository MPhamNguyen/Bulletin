package com.jdrms.bulletin.domain.reputation.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.network.SupabaseConfig
import com.jdrms.bulletin.domain.reputation.domain.model.Review
import com.jdrms.bulletin.domain.reputation.domain.model.RevieweeId
import com.jdrms.bulletin.domain.reputation.domain.repository.ReputationRepository

/**
 * Production implementation for Supabase-backed reputation data.
 * TODO: Implement Supabase PostgREST client calls once KMP Supabase SDK is wired up.
 */
class SupabaseReputationRepository(
    @Suppress("UnusedPrivateProperty")
    private val supabaseConfig: SupabaseConfig
) : ReputationRepository {

    override suspend fun getReviewsForUser(userId: RevieweeId): List<Review> {
        // TODO: Replace with Supabase client query when SDK is configured
        throw NotImplementedError(
            "Supabase integration pending SDK setup. Use InMemoryReputationRepository for development."
        )
    }

    override suspend fun submitReview(review: Review): Result<Review> {
        // TODO: Replace with Supabase client insert when SDK is configured
        return Result.Error(NotImplementedError("Supabase integration pending SDK setup."))
    }
}
