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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditProfilePhotoInteractionTest {
    @Test
    fun removalPersistsDefaultAvatarAndKeepsUnsavedTextEdits() = profileTest {
        val fixture = ProfileFixture()
        val profile = fixture.authenticateWithPhoto()
        val vm = viewModel(fixture)
        advanceUntilIdle()
        vm.onDraftChanged(vm.uiState.value.profileDraft.copy(fullName = "Updated Student"))

        vm.removePhoto()
        assertEquals(ProfilePhotoAction.REMOVING, vm.uiState.value.photoAction)
        advanceUntilIdle()

        assertNull(vm.uiState.value.profile?.avatarUrl)
        assertNull(assertIs<SessionState.Authenticated>(fixture.session.state.value).profile.avatarUrl)
        assertNull(fixture.profiles.getProfile(profile.id).getOrThrow()!!.avatarUrl)
        assertEquals("Updated Student", vm.uiState.value.profileDraft.fullName)
        assertTrue(vm.uiState.value.isProfileModified)
        assertFalse(vm.uiState.value.isPhotoBusy)
        vm.save {}
        advanceUntilIdle()
        assertNull(fixture.profiles.getProfile(profile.id).getOrThrow()!!.avatarUrl)
        assertEquals("Updated Student", vm.uiState.value.profile?.fullName)
    }

    @Test
    fun failedRemovalKeepsPhotoAndAllowsRetry() = profileTest {
        val fixture = ProfileFixture()
        val profile = fixture.authenticateWithPhoto()
        var fail = true
        val repository = object : ProfileRepository by fixture.profiles {
            override suspend fun updateProfile(profile: StudentProfile): Result<StudentProfile> =
                if (fail) Result.Error(Exception("Connection unavailable")) else fixture.profiles.updateProfile(profile)
        }
        val vm = viewModel(fixture, repository)
        advanceUntilIdle()

        vm.removePhoto()
        advanceUntilIdle()

        assertEquals(profile, vm.uiState.value.profile)
        assertEquals(profile, assertIs<SessionState.Authenticated>(fixture.session.state.value).profile)
        assertEquals("Connection unavailable", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isPhotoBusy)
        fail = false
        vm.removePhoto()
        advanceUntilIdle()
        assertNull(vm.uiState.value.profile?.avatarUrl)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun removalBlocksDuplicateRemovalUploadAndSaveUntilComplete() = profileTest {
        val fixture = ProfileFixture()
        val profile = fixture.authenticateWithPhoto()
        val pending = CompletableDeferred<Unit>()
        var updates = 0
        val repository = object : ProfileRepository by fixture.profiles {
            override suspend fun updateProfile(profile: StudentProfile): Result<StudentProfile> {
                updates++
                pending.await()
                return fixture.profiles.updateProfile(profile)
            }
        }
        var uploads = 0
        val photos = object : ProfilePhotoRepository {
            override suspend fun upload(userId: UserId, photo: ProfilePhoto): Result<ProfilePhotoUrl> {
                uploads++
                return Result.Success(ProfilePhotoUrl("memory://replacement"))
            }
        }
        val vm = viewModel(fixture, repository, photos)
        advanceUntilIdle()
        vm.removePhoto()
        runCurrent()
        vm.removePhoto()
        vm.uploadPhoto(jpegBytes(), "image/jpeg")
        var saved = false
        vm.save { saved = true }
        runCurrent()
        assertEquals(1, updates)
        assertEquals(0, uploads)
        assertEquals(profile.avatarUrl, vm.uiState.value.profile?.avatarUrl)
        pending.complete(Unit)
        advanceUntilIdle()
        assertFalse(saved)
        assertNull(vm.uiState.value.profile?.avatarUrl)
    }

    @Test
    fun removalIsIgnoredWhileUploading() = profileTest {
        val fixture = ProfileFixture()
        fixture.authenticateWithPhoto()
        val pending = CompletableDeferred<Unit>()
        val photos = object : ProfilePhotoRepository {
            override suspend fun upload(userId: UserId, photo: ProfilePhoto): Result<ProfilePhotoUrl> {
                pending.await()
                return Result.Success(ProfilePhotoUrl("memory://replacement"))
            }
        }
        val vm = viewModel(fixture, photos = photos)
        advanceUntilIdle()
        vm.uploadPhoto(jpegBytes(), "image/jpeg")
        runCurrent()
        vm.removePhoto()
        assertEquals(ProfilePhotoAction.UPLOADING, vm.uiState.value.photoAction)
        pending.complete(Unit)
        advanceUntilIdle()
        assertEquals("memory://replacement", vm.uiState.value.profile?.avatarUrl)
    }

    @Test
    fun profileSaveBlocksPhotoChanges() = profileTest {
        val fixture = ProfileFixture()
        val profile = fixture.authenticateWithPhoto()
        val pending = CompletableDeferred<Unit>()
        val repository = object : ProfileRepository by fixture.profiles {
            override suspend fun updateProfile(profile: StudentProfile): Result<StudentProfile> {
                pending.await()
                return fixture.profiles.updateProfile(profile)
            }
        }
        val vm = viewModel(fixture, repository)
        advanceUntilIdle()
        vm.onDraftChanged(vm.uiState.value.profileDraft.copy(fullName = "Updated Student"))
        vm.save {}
        runCurrent()
        vm.removePhoto()
        vm.uploadPhoto(jpegBytes(), "image/jpeg")
        assertNull(vm.uiState.value.photoAction)
        pending.complete(Unit)
        advanceUntilIdle()
        assertEquals(profile.avatarUrl, vm.uiState.value.profile?.avatarUrl)
    }

    @Test
    fun defaultAvatarNeedsNoRemovalRequest() = profileTest {
        val fixture = ProfileFixture()
        fixture.authenticate()
        val repository = object : ProfileRepository by fixture.profiles {
            override suspend fun updateProfile(profile: StudentProfile): Result<StudentProfile> =
                error("A profile without a photo should not be updated")
        }
        val vm = viewModel(fixture, repository)
        advanceUntilIdle()
        vm.removePhoto()
        advanceUntilIdle()
        assertNull(vm.uiState.value.profile?.avatarUrl)
        assertFalse(vm.uiState.value.isPhotoBusy)
    }

    @Test
    fun completingRemovalAfterSignOutDoesNotRestoreTheSession() = profileTest {
        val fixture = ProfileFixture()
        fixture.authenticateWithPhoto()
        val pending = CompletableDeferred<Unit>()
        val repository = object : ProfileRepository by fixture.profiles {
            override suspend fun updateProfile(profile: StudentProfile): Result<StudentProfile> {
                pending.await()
                return fixture.profiles.updateProfile(profile)
            }
        }
        val vm = viewModel(fixture, repository)
        advanceUntilIdle()
        vm.removePhoto()
        runCurrent()
        assertTrue(vm.uiState.value.isPhotoBusy)
        fixture.session.unauthenticated()
        runCurrent()
        assertFalse(vm.uiState.value.isPhotoBusy)
        pending.complete(Unit)
        advanceUntilIdle()
        assertIs<SessionState.Unauthenticated>(fixture.session.state.value)
        assertNull(vm.uiState.value.profile)
        assertFalse(vm.uiState.value.isPhotoBusy)
    }

    private suspend fun ProfileFixture.authenticateWithPhoto(): StudentProfile {
        val profile = authenticate().copy(avatarUrl = "memory://existing-photo")
        profiles.updateProfile(profile).getOrThrow()
        session.authenticated(profile)
        return profile
    }

    private fun viewModel(
        fixture: ProfileFixture,
        repository: ProfileRepository = fixture.profiles,
        photos: ProfilePhotoRepository = InMemoryProfilePhotoRepository()
    ) = EditProfileViewModel(
        fixture.session,
        UpdateStudentProfile(repository),
        UploadProfilePhoto(photos, repository),
        FlowUserMessenger()
    )

    private fun jpegBytes(): ByteArray = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0x01)
}
