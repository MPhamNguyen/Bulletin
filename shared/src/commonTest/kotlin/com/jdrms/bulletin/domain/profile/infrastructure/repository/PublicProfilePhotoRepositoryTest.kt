package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ProfileDto
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class PublicProfilePhotoRepositoryTest {
    @Test
    fun bothRepositoriesReturnAvatarOrNullWithoutLoadingReviews() = runTest {
        for (avatar in listOf("https://example.com/avatar.jpg", null)) {
            val memory = InMemoryProfileRepository(
                initialProfiles = mapOf(id.value to ProfileDto(id.value, "student@example.edu", avatarUrl = avatar)),
                initialReviews = emptyMap()
            )
            var requests = 0
            val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
                install(Postgrest)
                httpEngine = MockEngine { request ->
                    requests++
                    assertEquals("/rest/v1/profiles", request.url.encodedPath)
                    assertEquals("avatar_url", request.url.parameters["select"])
                    assertEquals("eq.${id.value}", request.url.parameters["id"])
                    val value = avatar?.let { "\"$it\"" } ?: "null"
                    respond("[{\"avatar_url\":$value}]", headers = headersOf("Content-Type", "application/json"))
                }
            }
            try {
                assertEquals(avatar, memory.getAvatarUrl(id).getOrThrow())
                assertNull(memory.getAvatarUrl(UserId("missing")).getOrThrow())
                assertEquals(avatar, SupabasePublicProfilePhotoRepository(client).getAvatarUrl(id).getOrThrow())
                assertEquals(1, requests)
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun missingProfileAndServerFailureAreDistinct() = runTest {
        for (status in listOf(HttpStatusCode.OK, HttpStatusCode.InternalServerError)) {
            val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
                install(Postgrest)
                httpEngine = MockEngine {
                    respond("[]", status, headersOf("Content-Type", "application/json"))
                }
            }
            try {
                val result = SupabasePublicProfilePhotoRepository(client).getAvatarUrl(id)
                if (status == HttpStatusCode.OK) assertNull(result.getOrThrow()) else assertIs<Result.Error>(result)
            } finally {
                client.close()
            }
        }
    }

    private companion object {
        val id = UserId("00000000-0000-0000-0000-000000000001")
    }
}
