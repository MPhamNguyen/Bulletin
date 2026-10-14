package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.GetBlockedUsers
import com.jdrms.bulletin.domain.profile.application.UnblockUser
import com.jdrms.bulletin.domain.profile.domain.model.UserBlock
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryUserBlockRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BlockedUsersViewModelTest {
    @Test
    fun loadAndUnblockRemoveTheSelectedUser() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = InMemoryUserBlockRepository()
            repository.block(UserBlock(UserId("current_student"), UserId("seller_102")))
            val auth = authenticatedAsCurrentUser()
            val viewModel = BlockedUsersViewModel(
                GetBlockedUsers(auth, repository, InMemoryProfileRepository()),
                UnblockUser(auth, repository)
            )

            viewModel.load()
            advanceUntilIdle()
            assertEquals(listOf("seller_102"), viewModel.uiState.value.users.map { it.id.value })

            viewModel.unblock("seller_102")
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.users.isEmpty())
            assertTrue(repository.blocks().isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun authenticatedAsCurrentUser(): AuthRepository = object : AuthRepository by InMemoryAuthRepository() {
        override suspend fun getCurrentUserId() = Result.Success(UserId("current_student"))
    }
}
