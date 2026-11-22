package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.PublicProfilePhotoRepository
import com.jdrms.bulletin.domain.profile.infrastructure.dto.PublicProfilePhotoDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException

class SupabasePublicProfilePhotoRepository internal constructor(
    private val supabase: SupabaseClient,
    private val readCache: ProfileReadCache,
    private val currentSessionId: () -> String? = { supabase.auth.currentUserOrNull()?.id }
) : PublicProfilePhotoRepository {
    constructor(supabase: SupabaseClient) : this(supabase, ProfileReadCache())

    override suspend fun getAvatarUrl(userId: UserId): Result<String?> {
        if (!SupabaseProfileRepository.isValidUuid(userId.value)) return Result.Success(null)
        return runCatching {
            readCache.observeSession(currentSessionId())
            readCache.publicPhotos.getOrLoad(userId) { loadAvatarUrl(userId) }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = {
                if (it is CancellationException) throw it
                Result.Error(Exception(ProfileErrorMapping.map(it), it))
            }
        )
    }

    override suspend fun getCachedAvatarUrl(userId: UserId): String? {
        readCache.observeSession(currentSessionId())
        return readCache.publicPhotos.getIfPresent(userId)
    }

    private suspend fun loadAvatarUrl(userId: UserId): String? =
        supabase.from(SupabaseProfileRepository.PROFILES_TABLE)
            .select(columns = Columns.raw("avatar_url")) {
                filter { eq("id", userId.value) }
            }.decodeSingleOrNull<PublicProfilePhotoDto>()?.avatarUrl
}
