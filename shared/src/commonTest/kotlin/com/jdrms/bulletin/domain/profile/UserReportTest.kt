package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.SubmitUserReport
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.model.UserReport
import com.jdrms.bulletin.domain.profile.domain.model.UserReportAlreadySubmittedException
import com.jdrms.bulletin.domain.profile.domain.model.UserReportReason
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.UserReportRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryUserReportRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class UserReportTest {
    @Test
    fun createsReportWithTrimmedOptionalDescription() {
        val report = UserReport.create(
            UserId("reporter"),
            UserId("target"),
            UserReportReason.SCAM_OR_FRAUD,
            "  Details  "
        )

        assertEquals("Details", assertIs<Result.Success<UserReport>>(report).data.description)
    }

    @Test
    fun rejectsSelfReports() {
        val result = UserReport.create(
            UserId("same"),
            UserId("same"),
            UserReportReason.SCAM_OR_FRAUD,
            ""
        )

        assertIs<Result.Error>(result)
    }

    @Test
    fun rejectsDescriptionOverMaximum() {
        val result = UserReport.create(
            UserId("reporter"),
            UserId("target"),
            UserReportReason.SCAM_OR_FRAUD,
            "x".repeat(UserReport.MAX_DESCRIPTION_LENGTH + 1)
        )

        assertIs<Result.Error>(result)
    }

    @Test
    fun submissionUsesAuthenticatedIdentityAndPersistsReport() = runTest {
        val reports = mutableListOf<UserReport>()
        val repository = object : UserReportRepository {
            override suspend fun submit(report: UserReport): Result<Unit> {
                reports += report
                return Result.Success(Unit)
            }
        }
        val auth = object : AuthRepository by InMemoryAuthRepository() {
            override suspend fun getCurrentUserId() = Result.Success(UserId("current"))
        }

        val result = SubmitUserReport(auth, repository)(
            UserId("target"),
            UserReportReason.FAKE_ACCOUNT_OR_IMPERSONATION,
            "Evidence"
        )

        assertIs<Result.Success<Unit>>(result)
        assertEquals("current", reports.single().reporterId.value)
        assertEquals("target", reports.single().reportedUserId.value)
    }

    @Test
    fun unauthenticatedUserCannotSubmitReport() = runTest {
        var submitted = false
        val auth = object : AuthRepository by InMemoryAuthRepository() {
            override suspend fun getCurrentUserId() = Result.Success(null)
        }
        val repository = object : UserReportRepository {
            override suspend fun submit(report: UserReport): Result<Unit> {
                submitted = true
                return Result.Success(Unit)
            }
        }

        val result = SubmitUserReport(auth, repository)(
            UserId("target"),
            UserReportReason.SCAM_OR_FRAUD,
            ""
        )

        assertIs<Result.Error>(result)
        assertEquals(false, submitted)
    }

    @Test
    fun selfReportNeverReachesRepository() = runTest {
        var submitted: UserReport? = null
        val auth = object : AuthRepository by InMemoryAuthRepository() {
            override suspend fun getCurrentUserId() = Result.Success(UserId("current"))
        }
        val repository = object : UserReportRepository {
            override suspend fun submit(report: UserReport): Result<Unit> {
                submitted = report
                return Result.Success(Unit)
            }
        }

        val result = SubmitUserReport(auth, repository)(
            UserId("current"),
            UserReportReason.SCAM_OR_FRAUD,
            ""
        )

        assertIs<Result.Error>(result)
        assertNull(submitted)
    }

    @Test
    fun rejectsDuplicateReportsFromSameReporterForSameUser() = runTest {
        val repository = InMemoryUserReportRepository()
        val firstReport = report("reporter", "target", UserReportReason.SCAM_OR_FRAUD)
        val duplicate = report("reporter", "target", UserReportReason.FAKE_ACCOUNT_OR_IMPERSONATION)

        assertIs<Result.Success<Unit>>(repository.submit(firstReport))
        val result = repository.submit(duplicate)

        assertIs<UserReportAlreadySubmittedException>(assertIs<Result.Error>(result).exception)
        assertEquals(listOf(firstReport), repository.reports())
    }

    @Test
    fun allowsNewReportAfterEarlierReportIsClosed() = runTest {
        val repository = InMemoryUserReportRepository()
        val closedReport = report("reporter", "target", UserReportReason.SCAM_OR_FRAUD)
        val newReport = report("reporter", "target", UserReportReason.FAKE_ACCOUNT_OR_IMPERSONATION)
        repository.submit(closedReport)

        repository.markClosedForTesting(closedReport)
        val result = repository.submit(newReport)

        assertIs<Result.Success<Unit>>(result)
        assertEquals(listOf(closedReport, newReport), repository.reports())
    }

    @Test
    fun allowsDifferentReportersToReportSameUser() = runTest {
        val repository = InMemoryUserReportRepository()

        assertIs<Result.Success<Unit>>(repository.submit(report("reporter-a", "target")))
        assertIs<Result.Success<Unit>>(repository.submit(report("reporter-b", "target")))

        assertEquals(2, repository.reports().size)
    }

    @Test
    fun allowsSameReporterToReportDifferentUsers() = runTest {
        val repository = InMemoryUserReportRepository()

        assertIs<Result.Success<Unit>>(repository.submit(report("reporter", "target-a")))
        assertIs<Result.Success<Unit>>(repository.submit(report("reporter", "target-b")))

        assertEquals(2, repository.reports().size)
    }

    private fun report(
        reporterId: String,
        reportedUserId: String,
        reason: UserReportReason = UserReportReason.SCAM_OR_FRAUD
    ) = UserReport.create(UserId(reporterId), UserId(reportedUserId), reason, "").getOrThrow()
}
