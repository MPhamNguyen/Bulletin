package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.GetBlockedUsers
import com.jdrms.bulletin.domain.profile.application.UnblockUser
import com.jdrms.bulletin.domain.profile.domain.model.UserBlock
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.UserBlockRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryUserBlockRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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

    @Test
    fun unblockCancelsAnOlderLoadThatCouldRestoreStaleUsers() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val pendingLoad = CompletableDeferred<Result<Set<UserId>>>()
            var loadCount = 0
            val repository = object : UserBlockRepository {
                override suspend fun block(block: UserBlock) = Result.Success(Unit)

                override suspend fun unblock(block: UserBlock) = Result.Success(Unit)

                override suspend fun getBlockedUserIds(viewerId: UserId): Result<Set<UserId>> {
                    loadCount++
                    return if (loadCount == 1) {
                        Result.Success(setOf(UserId("seller_102")))
                    } else {
                        pendingLoad.await()
                    }
                }

                override suspend fun getHiddenUserIds(viewerId: UserId) = Result.Success(emptySet<UserId>())
            }
            val auth = authenticatedAsCurrentUser()
            val viewModel = BlockedUsersViewModel(
                GetBlockedUsers(auth, repository, InMemoryProfileRepository()),
                UnblockUser(auth, repository)
            )

            viewModel.load()
            advanceUntilIdle()
            viewModel.load()
            runCurrent()

            viewModel.unblock("seller_102")
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.users.isEmpty())
            assertTrue(pendingLoad.isActive)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun authenticatedAsCurrentUser(): AuthRepository = object : AuthRepository by InMemoryAuthRepository() {
        override suspend fun getCurrentUserId() = Result.Success(UserId("current_student"))
    }
}
