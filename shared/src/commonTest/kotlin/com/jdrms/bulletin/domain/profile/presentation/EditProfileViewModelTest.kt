package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.FlowUserMessenger
import com.jdrms.bulletin.domain.profile.application.SessionState
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.UploadProfilePhoto
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfilePhotoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {
    @Test
    fun editingValidatesDraftAndPublishesSavedProfileToSession() = profileTest {
        val fixture = ProfileFixture()
        fixture.authenticate()
        val vm = EditProfileViewModel(
            fixture.session,
            UpdateStudentProfile(fixture.profiles),
            UploadProfilePhoto(InMemoryProfilePhotoRepository(), fixture.profiles),
            FlowUserMessenger()
        )
        advanceUntilIdle()
        val original = vm.uiState.value.profileDraft
        vm.onDraftChanged(original.copy(fullName = "Updated Student", university = "", universityIsCustom = true))
        vm.save {}
        assertEquals("Enter or select your school.", vm.uiState.value.profileFormErrors.school)
        vm.onDraftChanged(vm.uiState.value.profileDraft.copy(university = "Campus University"))
        var saved = false
        vm.save { saved = true }
        advanceUntilIdle()
        assertTrue(saved)
        assertEquals(
            "Updated Student",
            assertIs<SessionState.Authenticated>(fixture.session.state.value).profile.fullName
        )
        assertFalse(vm.uiState.value.isProfileModified)
    }

    @Test
    fun photoUploadRejectsInvalidBytesAndKeepsSavedProfile() = profileTest {
        val fixture = ProfileFixture()
        val profile = fixture.authenticate()
        val vm = EditProfileViewModel(
            fixture.session,
            UpdateStudentProfile(fixture.profiles),
            UploadProfilePhoto(InMemoryProfilePhotoRepository(), fixture.profiles),
            FlowUserMessenger()
        )
        advanceUntilIdle()
        vm.uploadPhoto(byteArrayOf(1, 2, 3), "image/jpeg")
        advanceUntilIdle()
        assertEquals(profile.avatarUrl, vm.uiState.value.profile?.avatarUrl)
        assertTrue(vm.uiState.value.errorMessage != null)
        vm.uploadPhoto(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0x01), "image/jpeg")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.profile?.avatarUrl?.startsWith("memory://profile-photo/") == true)
    }
}
