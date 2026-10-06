package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.generateUuid
import com.jdrms.bulletin.domain.profile.domain.model.ProfilePhoto
import com.jdrms.bulletin.domain.profile.domain.model.ProfilePhotoUrl
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.ProfilePhotoRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException

internal interface ProfilePhotoStorage {
    suspend fun upload(path: String, bytes: ByteArray, mediaType: String)
    fun publicUrl(path: String): String
}

class SupabaseProfilePhotoRepository internal constructor(
    private val storage: ProfilePhotoStorage,
    private val cacheKey: () -> String = ::generateUuid
) : ProfilePhotoRepository {
    constructor(supabase: SupabaseClient) : this(SupabaseProfilePhotoStorage(supabase))

    @Suppress("TooGenericExceptionCaught")
    override suspend fun upload(userId: UserId, photo: ProfilePhoto): Result<ProfilePhotoUrl> {
        val path = "${userId.value}/avatar"
        return try {
            storage.upload(path, photo.bytes, photo.mediaType.value)
            val publicUrl = storage.publicUrl(path)
            Result.Success(ProfilePhotoUrl("$publicUrl?v=${cacheKey()}"))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.Error(Exception(mapStorageErrorMessage(error), error))
        }
    }

    companion object {
        const val BUCKET_NAME = "pfp"

        fun mapStorageErrorMessage(error: Throwable): String {
            val message = error.message.orEmpty().lowercase()
            return when {
                message.contains("payload too large") || message.contains("maximum allowed size") ->
                    "Profile photos must be 5 MB or smaller."
                message.contains("mime type") || message.contains("content type") ->
                    "Choose a JPEG, PNG, or WebP image."
                message.contains("row-level security") || message.contains("unauthorized") ||
                    message.contains("forbidden") ->
                    "You do not have permission to upload this profile photo."
                message.contains("bucket") && message.contains("not found") ->
                    "The profile photo bucket is not configured."
                message.contains("unable to resolve host") || message.contains("failed to connect") ||
                    message.contains("timeout") ->
                    "Unable to upload the photo. Check your internet connection."
                else -> "Unable to upload the profile photo. Please try again."
            }
        }
    }
}

private class SupabaseProfilePhotoStorage(
    private val supabase: SupabaseClient
) : ProfilePhotoStorage {
    override suspend fun upload(path: String, bytes: ByteArray, mediaType: String) {
        supabase.storage.from(SupabaseProfilePhotoRepository.BUCKET_NAME).upload(path, bytes) {
            upsert = true
            contentType = ContentType.parse(mediaType)
        }
    }

    override fun publicUrl(path: String): String {
        return supabase.storage.from(SupabaseProfilePhotoRepository.BUCKET_NAME).publicUrl(path)
    }
}

class InMemoryProfilePhotoRepository : ProfilePhotoRepository {
    private val photos = mutableMapOf<UserId, ByteArray>()

    override suspend fun upload(userId: UserId, photo: ProfilePhoto): Result<ProfilePhotoUrl> {
        photos[userId] = photo.bytes
        return Result.Success(ProfilePhotoUrl("memory://profile-photo/${userId.value}"))
    }
}
