package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.domain.profile.infrastructure.dto.ProfileDto
import com.jdrms.bulletin.domain.profile.infrastructure.mapper.ProfileMapper
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseProfileRepository
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfilePersistenceFormatTest {

    @Test
    fun testUuidValidation() {
        assertTrue(SupabaseProfileRepository.isValidUuid("c3a81234-5678-4abc-9def-123456789abc"))
        assertTrue(SupabaseProfileRepository.isValidUuid("00000000-0000-0000-0000-000000000000"))
        assertFalse(SupabaseProfileRepository.isValidUuid("current_student"))
        assertFalse(SupabaseProfileRepository.isValidUuid("user_101"))
        assertFalse(SupabaseProfileRepository.isValidUuid(""))
        assertFalse(SupabaseProfileRepository.isValidUuid("not-a-uuid-at-all"))
    }

    @Test
    fun testProfileDtoDeletedAtSerialization() {
        val json = """
            {
                "id": "u1",
                "email": "test@csulb.edu",
                "deleted_at": "2026-10-06T19:00:00Z"
            }
        """.trimIndent()
        val dto = Json.decodeFromString<ProfileDto>(json)
        assertEquals("2026-10-06T19:00:00Z", dto.deletedAt)

        val domain = ProfileMapper.toDomain(dto)
        assertTrue(domain.isDeleted)
        assertEquals("2026-10-06T19:00:00Z", domain.deletedAt)

        val reEncodedDto = ProfileMapper.toDto(domain)
        assertEquals("2026-10-06T19:00:00Z", reEncodedDto.deletedAt)
    }
}
