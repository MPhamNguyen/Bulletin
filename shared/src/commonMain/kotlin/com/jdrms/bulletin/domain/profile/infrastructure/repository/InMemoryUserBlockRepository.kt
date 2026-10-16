package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.UserBlock
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.UserBlockRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryUserBlockRepository : UserBlockRepository {
    private val mutex = Mutex()
    private val blocks = mutableSetOf<UserBlock>()

    override suspend fun block(block: UserBlock): Result<Unit> = mutex.withLock {
        blocks += block
        Result.Success(Unit)
    }

    override suspend fun unblock(block: UserBlock): Result<Unit> = mutex.withLock {
        blocks -= block
        Result.Success(Unit)
    }

    override suspend fun getBlockedUserIds(viewerId: UserId): Result<Set<UserId>> = mutex.withLock {
        Result.Success(blocks.filter { it.blockerId == viewerId }.map { it.blockedUserId }.toSet())
    }

    override suspend fun getHiddenUserIds(viewerId: UserId): Result<Set<UserId>> = mutex.withLock {
        Result.Success(
            blocks.asSequence()
                .filter { it.blockerId == viewerId || it.blockedUserId == viewerId }
                .map { if (it.blockerId == viewerId) it.blockedUserId else it.blockerId }
                .toSet()
        )
    }

    suspend fun blocks(): Set<UserBlock> = mutex.withLock { blocks.toSet() }
}
