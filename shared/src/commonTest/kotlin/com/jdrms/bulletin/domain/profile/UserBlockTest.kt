package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.BlockUser
import com.jdrms.bulletin.domain.profile.application.BlockedUser
import com.jdrms.bulletin.domain.profile.application.GetBlockedUsers
import com.jdrms.bulletin.domain.profile.application.UnblockUser
import com.jdrms.bulletin.domain.profile.domain.model.UserBlock
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryUserBlockRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UserBlockTest {
    @Test
    fun rejectsBlockingOwnAccount() {
        val result = UserBlock.create(UserId("same"), UserId("same"))

        assertIs<Result.Error>(result)
    }

    @Test
    fun blockUseCaseUsesAuthenticatedIdentity() = runTest {
        val repository = InMemoryUserBlockRepository()
        val auth = object : AuthRepository by InMemoryAuthRepository() {
            override suspend fun getCurrentUserId() = Result.Success(UserId("blocker"))
        }

        val result = BlockUser(auth, repository)(UserId("blocked"))

        assertIs<Result.Success<Unit>>(result)
        assertEquals(
            setOf(UserBlock(UserId("blocker"), UserId("blocked"))),
            repository.blocks()
        )
    }

    @Test
    fun hiddenUsersIncludeBothSidesOfAStoredBlock() = runTest {
        val repository = InMemoryUserBlockRepository()
        repository.block(UserBlock(UserId("alice"), UserId("bob")))

        val aliceView = repository.getHiddenUserIds(UserId("alice"))
        val bobView = repository.getHiddenUserIds(UserId("bob"))

        assertEquals(setOf(UserId("bob")), assertIs<Result.Success<Set<UserId>>>(aliceView).data)
        assertEquals(setOf(UserId("alice")), assertIs<Result.Success<Set<UserId>>>(bobView).data)
        assertTrue(assertIs<Result.Success<Set<UserId>>>(repository.getHiddenUserIds(UserId("carol"))).data.isEmpty())
    }

    @Test
    fun unblockUseCaseUsesAuthenticatedIdentity() = runTest {
        val repository = InMemoryUserBlockRepository()
        repository.block(UserBlock(UserId("blocker"), UserId("blocked")))
        val auth = object : AuthRepository by InMemoryAuthRepository() {
            override suspend fun getCurrentUserId() = Result.Success(UserId("blocker"))
        }

        val result = UnblockUser(auth, repository)(UserId("blocked"))

        assertIs<Result.Success<Unit>>(result)
        assertTrue(repository.blocks().isEmpty())
    }

    @Test
    fun blockedUserIdsIncludeOnlyOutgoingRelationships() = runTest {
        val repository = InMemoryUserBlockRepository()
        repository.block(UserBlock(UserId("alice"), UserId("bob")))
        repository.block(UserBlock(UserId("carol"), UserId("alice")))

        assertEquals(
            setOf(UserId("bob")),
            assertIs<Result.Success<Set<UserId>>>(repository.getBlockedUserIds(UserId("alice"))).data
        )
    }

    @Test
    fun blockedUsersUseProfileNamesForTheSettingsList() = runTest {
        val blocks = InMemoryUserBlockRepository()
        blocks.block(UserBlock(UserId("current_student"), UserId("seller_102")))
        val auth = object : AuthRepository by InMemoryAuthRepository() {
            override suspend fun getCurrentUserId() = Result.Success(UserId("current_student"))
        }

        val result = GetBlockedUsers(auth, blocks, InMemoryProfileRepository())()

        val users = assertIs<Result.Success<List<BlockedUser>>>(result).data
        assertEquals(listOf("Sean Gallagher"), users.map { it.profile?.fullName })
    }
}
