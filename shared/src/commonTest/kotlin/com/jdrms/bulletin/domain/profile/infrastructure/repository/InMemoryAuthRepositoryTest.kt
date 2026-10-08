package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

class InMemoryAuthRepositoryTest {
    @Test
    fun credentialsWithoutAnExplicitProfileMappingCannotUseAnotherSeededIdentity() = runTest {
        val auth = InMemoryAuthRepository(
            profileRepository = InMemoryProfileRepository(),
            initialCredentials = mapOf("other@school.edu" to "password123")
        )
        assertIs<Result.Error>(auth.login(StudentEmail("other@school.edu"), "password123"))
    }
}
