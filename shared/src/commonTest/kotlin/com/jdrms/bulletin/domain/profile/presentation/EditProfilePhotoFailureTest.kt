package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.FlowUserMessenger
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.SessionState
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.UploadProfilePhoto
import com.jdrms.bulletin.domain.profile.domain.model.ProfilePhoto
import com.jdrms.bulletin.domain.profile.domain.model.ProfilePhotoUrl
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.ProfilePhotoRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfilePhotoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class EditProfilePhotoFailureTest {
    @Test
    fun thrownRemovalKeepsSavedPhotoAndAllowsRetry() = profileTest {
        val fixture = ProfileFixture()
        val profile = fixture.authenticate().copy(avatarUrl = "memory://existing-photo")
        fixture.profiles.updateProfile(profile).getOrThrow()
        fixture.session.authenticated(profile)
        var fail = true
        val repository = object : ProfileRepository by fixture.profiles {
            override suspend fun updateProfile(profile: StudentProfile): Result<StudentProfile> {
                if (fail) error("Connection unavailable")
                return fixture.profiles.updateProfile(profile)
            }
        }
        val vm = EditProfileViewModel(
            fixture.session,
            UpdateStudentProfile(repository),
            UploadProfilePhoto(InMemoryProfilePhotoRepository(), repository),
            FlowUserMessenger()
        )
        advanceUntilIdle()
        vm.removePhoto()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isPhotoBusy)
        assertEquals("Connection unavailable", vm.uiState.value.errorMessage)
        assertEquals(profile, vm.uiState.value.profile)
        assertEquals(profile, assertIs<SessionState.Authenticated>(fixture.session.state.value).profile)
        assertEquals(profile.avatarUrl, fixture.profiles.getProfile(profile.id).getOrThrow()!!.avatarUrl)
        fail = false
        vm.removePhoto()
        advanceUntilIdle()
        assertNull(vm.uiState.value.profile?.avatarUrl)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun thrownUploadClearsBusyStateAndAllowsRetry() = profileTest {
        val fixture = ProfileFixture()
        val profile = fixture.authenticate()
        var fail = true
        val photos = object : ProfilePhotoRepository {
            override suspend fun upload(userId: UserId, photo: ProfilePhoto): Result<ProfilePhotoUrl> {
                if (fail) throw UnsupportedOperationException()
                return Result.Success(ProfilePhotoUrl("memory://replacement"))
            }
        }
        val vm = EditProfileViewModel(
            fixture.session,
            UpdateStudentProfile(fixture.profiles),
            UploadProfilePhoto(photos, fixture.profiles),
            FlowUserMessenger()
        )
        advanceUntilIdle()
        val bytes = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0x01)
        vm.uploadPhoto(bytes, "image/jpeg")
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isPhotoBusy)
        assertEquals("Failed to upload profile photo", vm.uiState.value.errorMessage)
        assertEquals(profile, vm.uiState.value.profile)
        fail = false
        vm.uploadPhoto(bytes, "image/jpeg")
        advanceUntilIdle()
        assertEquals("memory://replacement", vm.uiState.value.profile?.avatarUrl)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun cancellationClearsBusyStateWithoutShowingFailure() = profileTest {
        val fixture = ProfileFixture()
        val profile = fixture.authenticate().copy(avatarUrl = "memory://existing-photo")
        fixture.session.authenticated(profile)
        val repository = object : ProfileRepository by fixture.profiles {
            override suspend fun updateProfile(profile: StudentProfile): Result<StudentProfile> =
                throw CancellationException("Photo operation cancelled")
        }
        val vm = EditProfileViewModel(
            fixture.session,
            UpdateStudentProfile(repository),
            UploadProfilePhoto(InMemoryProfilePhotoRepository(), repository),
            FlowUserMessenger()
        )
        advanceUntilIdle()
        vm.removePhoto()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isPhotoBusy)
        assertNull(vm.uiState.value.errorMessage)
        assertEquals(profile, vm.uiState.value.profile)
        assertEquals(profile, assertIs<SessionState.Authenticated>(fixture.session.state.value).profile)
    }
}
