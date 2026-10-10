package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.UserReport
import com.jdrms.bulletin.domain.profile.domain.repository.UserReportRepository
import com.jdrms.bulletin.domain.profile.infrastructure.dto.UserReportInsertDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException

class SupabaseUserReportRepository internal constructor(
    private val table: SupabaseUserReportsTable
) : UserReportRepository {
    constructor(supabase: SupabaseClient) : this(PostgrestSupabaseUserReportsTable(supabase))

    @Suppress("TooGenericExceptionCaught")
    override suspend fun submit(report: UserReport): Result<Unit> = try {
        val authenticatedReporter = table.authenticatedUserId()
            ?.takeIf(String::isNotBlank)
            ?: return Result.Error(IllegalStateException("Sign in to report a user."))
        if (authenticatedReporter != report.reporterId.value) {
            return Result.Error(IllegalStateException("Report identity does not match the signed-in user."))
        }
        table.insert(
            UserReportInsertDto(
                reporterId = authenticatedReporter,
                reportedUserId = report.reportedUserId.value,
                reason = report.reason.label,
                description = report.description
            )
        )
        Result.Success(Unit)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: PostgrestRestException) {
        Result.Error(UserReportPersistenceFailureMapper.map(exception.code, exception))
    } catch (exception: Exception) {
        Result.Error(IllegalStateException("We couldn't submit your report right now. Please try again.", exception))
    }

    companion object {
        const val TABLE = "user_reports"
    }
}

internal interface SupabaseUserReportsTable {
    suspend fun authenticatedUserId(): String?
    suspend fun insert(report: UserReportInsertDto)
}

private class PostgrestSupabaseUserReportsTable(
    private val supabase: SupabaseClient
) : SupabaseUserReportsTable {
    override suspend fun authenticatedUserId(): String? {
        supabase.auth.awaitInitialization()
        return supabase.auth.currentUserOrNull()?.id
    }

    override suspend fun insert(report: UserReportInsertDto) {
        supabase.from(SupabaseUserReportRepository.TABLE).insert(report)
    }
}
