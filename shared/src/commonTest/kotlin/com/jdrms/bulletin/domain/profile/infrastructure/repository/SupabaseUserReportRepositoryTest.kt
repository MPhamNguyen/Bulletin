package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.model.UserReport
import com.jdrms.bulletin.domain.profile.domain.model.UserReportAlreadySubmittedException
import com.jdrms.bulletin.domain.profile.infrastructure.dto.SupabaseUserReportType
import com.jdrms.bulletin.domain.profile.infrastructure.dto.UserReportInsertDto
import com.jdrms.bulletin.domain.reporting.domain.model.ReportReason
import com.jdrms.bulletin.domain.reporting.domain.model.ReportType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
    fun insertPayloadLeavesModerationStatusToDatabase() {
        val payload = Json.encodeToString(
            UserReportInsertDto(
                reporterId = "reporter-id",
                reportedUserId = "target-id",
                reason = "Scam or Fraud",
                description = "Details",
                type = SupabaseUserReportType.USER
            )
        )

        assertFalse(payload.contains("\"status\""))
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
                type = SupabaseUserReportType.USER
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

    @Test
    fun preservesEveryReportReasonWhenWritingTheUserReportType() = runTest {
        val table = FakeTable("reporter-id")
        val repository = SupabaseUserReportRepository(table)

        ReportReason.forType(ReportType.USER).forEach { reason ->
            val report = UserReport.create(UserId("reporter-id"), UserId("target-id"), reason, "Details")
                .getOrThrow()

            repository.submit(report).getOrThrow()

            assertEquals(reason.label, table.inserted?.reason)
            assertEquals(SupabaseUserReportType.USER, table.inserted?.type)
            assertEquals("reporter-id", table.inserted?.reporterId)
            assertEquals("target-id", table.inserted?.reportedUserId)
        }
    }

    @Test
    fun preservesEmptyAndMaximumLengthDescriptions() = runTest {
        val table = FakeTable("reporter-id")
        val repository = SupabaseUserReportRepository(table)

        listOf("", "x".repeat(UserReport.MAX_DESCRIPTION_LENGTH)).forEach { description ->
            repository.submit(report(description)).getOrThrow()

            assertEquals(description, table.inserted?.description)
            assertEquals(SupabaseUserReportType.USER, table.inserted?.type)
        }
    }

    @Test
    fun blankAuthenticatedIdentityDoesNotWriteAReport() = runTest {
        val table = FakeTable("  ")

        val result = SupabaseUserReportRepository(table).submit(report())

        assertIs<Result.Error>(result)
        assertNull(table.inserted)
    }

    @Test
    fun cancellationIsRethrownWithoutRecordingAReport() = runTest {
        val table = object : FakeTable("reporter-id") {
            override suspend fun insert(report: UserReportInsertDto) {
                throw CancellationException("Submission cancelled")
            }
        }

        assertFailsWith<CancellationException> { SupabaseUserReportRepository(table).submit(report()) }
        assertNull(table.inserted)
    }

    private fun report(description: String = "Details") = UserReport.create(
        UserId("reporter-id"),
        UserId("target-id"),
        ReportReason.SCAM_OR_FRAUD,
        description
    ).getOrThrow()

    private open class FakeTable(private val sessionUserId: String?) : SupabaseUserReportsTable {
        var inserted: UserReportInsertDto? = null
        override suspend fun authenticatedUserId(): String? = sessionUserId
        override suspend fun insert(report: UserReportInsertDto) {
            inserted = report
        }
    }
}
