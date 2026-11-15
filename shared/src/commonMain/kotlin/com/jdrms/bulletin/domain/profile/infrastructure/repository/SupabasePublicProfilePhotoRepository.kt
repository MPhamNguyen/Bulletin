package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.PublicProfilePhotoRepository
import com.jdrms.bulletin.domain.profile.infrastructure.dto.PublicProfilePhotoDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class SupabasePublicProfilePhotoRepository(
    private val supabase: SupabaseClient
) : PublicProfilePhotoRepository {
    override suspend fun getAvatarUrl(userId: UserId): Result<String?> {
        if (!SupabaseProfileRepository.isValidUuid(userId.value)) return Result.Success(null)
        return runCatching {
            supabase.from(SupabaseProfileRepository.PROFILES_TABLE)
                .select(columns = Columns.raw("avatar_url")) {
                    filter { eq("id", userId.value) }
                }.decodeSingleOrNull<PublicProfilePhotoDto>()?.avatarUrl
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(Exception(ProfileErrorMapping.map(it), it)) }
        )
    }
}
