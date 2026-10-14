package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.UserBlock
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.infrastructure.dto.UserBlockInsertDto
import com.jdrms.bulletin.domain.profile.infrastructure.dto.UserBlockRowDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class SupabaseUserBlockRepositoryTest {
    @Test
    fun insertPayloadContainsOnlyBlockRelationshipFields() {
        val payload = Json.encodeToString(UserBlockInsertDto("blocker", "blocked"))

        assertFalse(payload.contains("created_at"))
        assertEquals("{\"blocker_id\":\"blocker\",\"blocked_user_id\":\"blocked\"}", payload)
    }

    @Test
    fun insertsExpectedRelationshipForAuthenticatedUser() = runTest {
        val table = FakeTable("blocker")

        val result = SupabaseUserBlockRepository(table).block(UserBlock(UserId("blocker"), UserId("blocked")))

        assertIs<Result.Success<Unit>>(result)
        assertEquals(UserBlockInsertDto("blocker", "blocked"), table.inserted)
    }

    @Test
    fun rejectsRelationshipClaimedForAnotherSession() = runTest {
        val table = FakeTable("different-user")

        val result = SupabaseUserBlockRepository(table).block(UserBlock(UserId("blocker"), UserId("blocked")))

        assertIs<Result.Error>(result)
        assertNull(table.inserted)
    }

    @Test
    fun returnsIncomingAndOutgoingHiddenUsers() = runTest {
        val table = FakeTable("viewer").apply {
            rows = listOf(
                UserBlockRowDto("viewer", "outgoing"),
                UserBlockRowDto("incoming", "viewer")
            )
        }

        val result = SupabaseUserBlockRepository(table).getHiddenUserIds(UserId("viewer"))

        assertEquals(
            setOf(UserId("outgoing"), UserId("incoming")),
            assertIs<Result.Success<Set<UserId>>>(result).data
        )
    }

    @Test
    fun returnsOnlyUsersExplicitlyBlockedByViewer() = runTest {
        val table = FakeTable("viewer").apply {
            rows = listOf(UserBlockRowDto("viewer", "blocked"))
        }

        val result = SupabaseUserBlockRepository(table).getBlockedUserIds(UserId("viewer"))

        assertEquals(
            setOf(UserId("blocked")),
            assertIs<Result.Success<Set<UserId>>>(result).data
        )
    }

    @Test
    fun deletesExpectedRelationshipForAuthenticatedUser() = runTest {
        val table = FakeTable("blocker")

        val result = SupabaseUserBlockRepository(table).unblock(UserBlock(UserId("blocker"), UserId("blocked")))

        assertIs<Result.Success<Unit>>(result)
        assertEquals(UserBlockInsertDto("blocker", "blocked"), table.deleted)
    }

    private open class FakeTable(private val sessionUserId: String?) : SupabaseUserBlocksTable {
        var inserted: UserBlockInsertDto? = null
        var deleted: UserBlockInsertDto? = null
        var rows: List<UserBlockRowDto> = emptyList()
        override suspend fun authenticatedUserId(): String? = sessionUserId
        override suspend fun insert(block: UserBlockInsertDto) {
            inserted = block
        }
        override suspend fun delete(block: UserBlockInsertDto) {
            deleted = block
        }
        override suspend fun findOutgoing(viewerId: String): List<UserBlockRowDto> = rows.filter {
            it.blockerId == viewerId
        }
        override suspend fun findForViewer(viewerId: String): List<UserBlockRowDto> = rows
    }
}
