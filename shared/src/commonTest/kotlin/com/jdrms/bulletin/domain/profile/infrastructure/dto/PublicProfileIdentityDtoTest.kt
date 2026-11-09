package com.jdrms.bulletin.domain.profile.infrastructure.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PublicProfileIdentityDtoTest {
    @Test
    fun decodesPublicIdentityWithoutPrivateEmailField() {
        val identity = Json.decodeFromString<PublicProfileIdentityDto>(
            """{"id":"reviewer-id","full_name":"Real Reviewer","avatar_url":null}"""
        )

        assertEquals("Real Reviewer", identity.fullName)
    }
}
