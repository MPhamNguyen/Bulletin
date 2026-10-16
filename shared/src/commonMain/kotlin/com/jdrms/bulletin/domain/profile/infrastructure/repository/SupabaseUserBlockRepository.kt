package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.UserBlock
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.UserBlockRepository
import com.jdrms.bulletin.domain.profile.infrastructure.dto.UserBlockInsertDto
import com.jdrms.bulletin.domain.profile.infrastructure.dto.UserBlockRowDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException

@Suppress("TooGenericExceptionCaught")
class SupabaseUserBlockRepository internal constructor(
    private val table: SupabaseUserBlocksTable
) : UserBlockRepository {
    constructor(supabase: SupabaseClient) : this(PostgrestSupabaseUserBlocksTable(supabase))

    override suspend fun block(block: UserBlock): Result<Unit> = try {
        val authenticatedUserId = table.authenticatedUserId()
            ?.takeIf(String::isNotBlank)
            ?: return Result.Error(IllegalStateException("Sign in to block a user."))
        if (authenticatedUserId != block.blockerId.value) {
            return Result.Error(IllegalStateException("Block identity does not match the signed-in user."))
        }
        table.insert(UserBlockInsertDto(authenticatedUserId, block.blockedUserId.value))
        Result.Success(Unit)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: PostgrestRestException) {
        Result.Error(IllegalStateException("We couldn't block this user right now. Please try again.", exception))
    } catch (exception: Exception) {
        Result.Error(IllegalStateException("We couldn't block this user right now. Please try again.", exception))
    }

    override suspend fun unblock(block: UserBlock): Result<Unit> = try {
        val authenticatedUserId = table.authenticatedUserId()
            ?.takeIf(String::isNotBlank)
            ?: return Result.Error(IllegalStateException("Sign in to unblock a user."))
        if (authenticatedUserId != block.blockerId.value) {
            return Result.Error(IllegalStateException("Block identity does not match the signed-in user."))
        }
        table.delete(UserBlockInsertDto(authenticatedUserId, block.blockedUserId.value))
        Result.Success(Unit)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.Error(IllegalStateException("We couldn't unblock this user right now. Please try again.", exception))
    }

    override suspend fun getBlockedUserIds(viewerId: UserId): Result<Set<UserId>> = try {
        val authenticatedUserId = table.authenticatedUserId()
            ?.takeIf(String::isNotBlank)
            ?: return Result.Success(emptySet())
        if (authenticatedUserId != viewerId.value) {
            return Result.Error(IllegalStateException("Block identity does not match the signed-in user."))
        }
        Result.Success(table.findOutgoing(authenticatedUserId).map { UserId(it.blockedUserId) }.toSet())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.Error(IllegalStateException("We couldn't load blocked users right now. Please try again.", exception))
    }

    override suspend fun getHiddenUserIds(viewerId: UserId): Result<Set<UserId>> = try {
        val authenticatedUserId = table.authenticatedUserId()
            ?.takeIf(String::isNotBlank)
            ?: return Result.Success(emptySet())
        if (authenticatedUserId != viewerId.value) {
            return Result.Error(IllegalStateException("Block identity does not match the signed-in user."))
        }
        Result.Success(
            table.findForViewer(authenticatedUserId)
                .flatMap { row ->
                    listOfNotNull(
                        row.blockedUserId.takeUnless { it == authenticatedUserId },
                        row.blockerId.takeUnless { it == authenticatedUserId }
                    )
                }
                .map(::UserId)
                .toSet()
        )
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.Error(IllegalStateException("We couldn't load blocked users right now. Please try again.", exception))
    }

    companion object {
        const val TABLE = "user_blocks"
    }
}

internal interface SupabaseUserBlocksTable {
    suspend fun authenticatedUserId(): String?
    suspend fun insert(block: UserBlockInsertDto)
    suspend fun delete(block: UserBlockInsertDto)
    suspend fun findOutgoing(viewerId: String): List<UserBlockRowDto>
    suspend fun findForViewer(viewerId: String): List<UserBlockRowDto>
}

private class PostgrestSupabaseUserBlocksTable(
    private val supabase: SupabaseClient
) : SupabaseUserBlocksTable {
    override suspend fun authenticatedUserId(): String? {
        supabase.auth.awaitInitialization()
        return supabase.auth.currentUserOrNull()?.id
    }

    override suspend fun insert(block: UserBlockInsertDto) {
        supabase.from(SupabaseUserBlockRepository.TABLE).insert(block)
    }

    override suspend fun delete(block: UserBlockInsertDto) {
        supabase.from(SupabaseUserBlockRepository.TABLE).delete {
            filter {
                eq("blocker_id", block.blockerId)
                eq("blocked_user_id", block.blockedUserId)
            }
        }
    }

    override suspend fun findOutgoing(viewerId: String): List<UserBlockRowDto> {
        return supabase.from(SupabaseUserBlockRepository.TABLE).select {
            filter { eq("blocker_id", viewerId) }
        }.decodeList<UserBlockRowDto>()
    }

    override suspend fun findForViewer(viewerId: String): List<UserBlockRowDto> {
        val outgoing = supabase.from(SupabaseUserBlockRepository.TABLE).select {
            filter { eq("blocker_id", viewerId) }
        }.decodeList<UserBlockRowDto>()
        val incoming = supabase.from(SupabaseUserBlockRepository.TABLE).select {
            filter { eq("blocked_user_id", viewerId) }
        }.decodeList<UserBlockRowDto>()
        return outgoing + incoming
    }
}
