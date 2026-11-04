package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ProfileDto
import com.jdrms.bulletin.domain.profile.infrastructure.mapper.ProfileMapper
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfilePhotoRemovalContractTest {
    @Test
    fun removalPersistsAcrossRefreshInBothRepositories() = runTest {
        val table = ProfileTable()
        try {
            for (repository in table.repositories()) {
                val saved = removePhoto(repository).getOrThrow()
                assertEquals(profile.copy(avatarUrl = null), saved)
                assertNull(repository.refreshProfile(profile.id).getOrThrow()!!.avatarUrl)
            }
            assertTrue(table.receivedAvatarNull)
        } finally {
            table.client.close()
        }
    }

    @Test
    fun aNewPhotoCanBeSavedAfterRemovalInBothRepositories() = runTest {
        val table = ProfileTable()
        try {
            for (repository in table.repositories()) {
                val cleared = removePhoto(repository).getOrThrow()
                UpdateStudentProfile(repository)(
                    cleared,
                    cleared.fullName,
                    cleared.major,
                    cleared.university,
                    cleared.bio,
                    avatarUrl = "https://example.com/replacement.jpg"
                ).getOrThrow()
                assertEquals(
                    "https://example.com/replacement.jpg",
                    repository.refreshProfile(profile.id).getOrThrow()!!.avatarUrl
                )
            }
        } finally {
            table.client.close()
        }
    }

    @Test
    fun failedSupabaseRemovalReturnsFailureAndKeepsSavedPhoto() = runTest {
        val table = ProfileTable(failUpdate = true)
        try {
            val repository = SupabaseProfileRepository(table.client)
            assertIs<Result.Error>(removePhoto(repository))
            assertEquals(profile.avatarUrl, repository.refreshProfile(profile.id).getOrThrow()!!.avatarUrl)
        } finally {
            table.client.close()
        }
    }

    @Test
    fun zeroRowSupabaseRemovalReturnsFailureAndKeepsSavedPhoto() = runTest {
        val table = ProfileTable(skipUpdate = true)
        try {
            val repository = SupabaseProfileRepository(table.client)
            val result = assertIs<Result.Error>(removePhoto(repository))
            assertEquals("Profile update did not save any changes. Please try again.", result.message)
            assertTrue(table.receivedAvatarNull)
            assertEquals(profile.avatarUrl, repository.refreshProfile(profile.id).getOrThrow()!!.avatarUrl)
        } finally {
            table.client.close()
        }
    }

    private suspend fun removePhoto(repository: ProfileRepository): Result<StudentProfile> =
        UpdateStudentProfile(repository)(
            profile,
            profile.fullName,
            profile.major,
            profile.university,
            profile.bio,
            avatarUrl = null
        )

    private class ProfileTable(failUpdate: Boolean = false, skipUpdate: Boolean = false) {
        private var stored = Json.encodeToJsonElement(ProfileDto.serializer(), ProfileMapper.toDto(profile)).jsonObject
        var receivedAvatarNull = false
            private set
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Auth) {
                minimalSettings()
            }
            install(Postgrest)
            httpEngine = MockEngine { request ->
                val headers = headersOf("Content-Type", "application/json")
                when {
                    request.url.encodedPath != "/rest/v1/profiles" -> respond("[]", headers = headers)
                    request.method == HttpMethod.Patch -> {
                        assertEquals("eq.${profile.id.value}", request.url.parameters["id"])
                        assertTrue(request.headers["Prefer"].orEmpty().contains("return=representation"))
                        val body = Json.parseToJsonElement(assertIs<TextContent>(request.body).text).jsonObject
                        assertTrue("avatar_url" in body, "Photo removal must send avatar_url explicitly")
                        receivedAvatarNull = body["avatar_url"] == JsonNull
                        if (failUpdate) {
                            respond(
                                """{"code":"42501","message":"row-level security denied"}""",
                                HttpStatusCode.Forbidden, headers
                            )
                        } else if (skipUpdate) {
                            respond("[]", HttpStatusCode.OK, headers)
                        } else {
                            stored = kotlinx.serialization.json.JsonObject(stored + body)
                            respond("[$stored]", HttpStatusCode.OK, headers)
                        }
                    }
                    else -> respond("[$stored]", headers = headers)
                }
            }
        }

        fun repositories(): List<ProfileRepository> = listOf(
            InMemoryProfileRepository(
                initialProfiles = mapOf(profile.id.value to ProfileMapper.toDto(profile)),
                initialReviews = emptyMap()
            ),
            SupabaseProfileRepository(client)
        )
    }

    companion object {
        private val profile = StudentProfile(
            id = UserId("c3a81234-5678-4abc-9def-123456789abc"),
            email = StudentEmail("student@school.edu"),
            fullName = "Test Student",
            avatarUrl = "https://example.com/existing.jpg"
        )
    }
}
