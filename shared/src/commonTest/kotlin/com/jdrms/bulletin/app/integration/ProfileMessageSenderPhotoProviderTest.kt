package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.PublicProfilePhotoRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileMessageSenderPhotoProviderTest {
    @Test
    fun translatesSenderIdentityAndReturnsAvatar() = runTest {
        val provider = ProfileMessageSenderPhotoProvider(
            PublicProfilePhotoRepository { id ->
                assertEquals(UserId("sender"), id)
                Result.Success("https://example.com/avatar.jpg")
            }
        )
        assertEquals("https://example.com/avatar.jpg", provider.photoUrl(SenderId("sender")))
    }

    @Test
    fun missingAvatarAndLookupFailureReturnNull() = runTest {
        for (result in listOf(Result.Success(null), Result.Error(Exception("unavailable")))) {
            val provider = ProfileMessageSenderPhotoProvider(PublicProfilePhotoRepository { result })
            assertNull(provider.photoUrl(SenderId("sender")))
        }
    }
}
