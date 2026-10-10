package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.model.UserReport
import com.jdrms.bulletin.domain.profile.domain.model.UserReportAlreadySubmittedException
import com.jdrms.bulletin.domain.profile.domain.model.UserReportReason
import com.jdrms.bulletin.domain.profile.infrastructure.dto.UserReportInsertDto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SupabaseUserReportRepositoryTest {
    @Test
    fun mapsUniqueConstraintViolationToDuplicateReportFailure() {
        val cause = IllegalStateException("duplicate report")

        val result = UserReportPersistenceFailureMapper.map("23505", cause)

        assertIs<UserReportAlreadySubmittedException>(result)
    }

    @Test
    fun mapsOtherPostgrestFailuresToSafePersistenceFailure() {
        val cause = IllegalStateException("database detail")

        val result = UserReportPersistenceFailureMapper.map("42501", cause)

        assertEquals("We couldn't submit your report right now. Please try again.", result.message)
        assertEquals(cause, result.cause)
    }

    @Test
    fun insertsExpectedColumnsAndReason() = runTest {
        val table = FakeTable("reporter-id")

        val result = SupabaseUserReportRepository(table).submit(report())

        assertIs<Result.Success<Unit>>(result)
        assertEquals(
            UserReportInsertDto(
                reporterId = "reporter-id",
                reportedUserId = "target-id",
                reason = "Scam or Fraud",
                description = "Details",
                status = "pending"
            ),
            table.inserted
        )
    }

    @Test
    fun rejectsClaimedReporterThatDoesNotMatchSession() = runTest {
        val table = FakeTable("different-user")

        val result = SupabaseUserReportRepository(table).submit(report())

        assertIs<Result.Error>(result)
        assertNull(table.inserted)
    }

    @Test
    fun requiresAuthenticatedSession() = runTest {
        val table = FakeTable(null)

        val result = SupabaseUserReportRepository(table).submit(report())

        assertIs<Result.Error>(result)
        assertNull(table.inserted)
    }

    @Test
    fun mapsStorageFailureToSafeResultError() = runTest {
        val table = object : FakeTable("reporter-id") {
            override suspend fun insert(report: UserReportInsertDto) {
                error("database detail")
            }
        }

        val result = SupabaseUserReportRepository(table).submit(report())

        assertEquals(
            "We couldn't submit your report right now. Please try again.",
            assertIs<Result.Error>(result).message
        )
    }

    private fun report() = UserReport.create(
        UserId("reporter-id"),
        UserId("target-id"),
        UserReportReason.SCAM_OR_FRAUD,
        "Details"
    ).getOrThrow()

    private open class FakeTable(private val sessionUserId: String?) : SupabaseUserReportsTable {
        var inserted: UserReportInsertDto? = null
        override suspend fun authenticatedUserId(): String? = sessionUserId
        override suspend fun insert(report: UserReportInsertDto) {
            inserted = report
        }
    }
}
