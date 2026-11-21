package com.jdrms.bulletin.app.di

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppContainerSessionResolvedTest {
    @Test
    fun session_is_unresolved_until_the_stored_session_has_been_restored() = runTest {
        val container = AppContainer(isInspectionMode = true)

        assertFalse(container.isSessionResolved.first())

        container.profile.sessionRepository.restore()

        assertTrue(container.isSessionResolved.first())
    }
}
