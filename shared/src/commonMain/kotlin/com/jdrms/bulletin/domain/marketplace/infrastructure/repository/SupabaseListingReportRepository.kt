package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingReport
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingReportAlreadySubmittedException
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingReportSelfReportException
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import com.jdrms.bulletin.domain.marketplace.domain.repository.ListingReportRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.ListingReportInsertDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.ListingReportListingIdDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.ListingReportStatusDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.SupabaseListingReportType
import com.jdrms.bulletin.domain.reporting.domain.model.ReportStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException

class SupabaseListingReportRepository internal constructor(
    private val table: SupabaseListingReportsTable
) : ListingReportRepository {
    constructor(supabase: SupabaseClient) : this(PostgrestSupabaseListingReportsTable(supabase))

    @Suppress("TooGenericExceptionCaught")
    override suspend fun submit(report: ListingReport): Result<Unit> = try {
        val authenticatedReporter = table.authenticatedUserId()
            ?.takeIf(String::isNotBlank)
            ?: return Result.Error(IllegalStateException("Sign in to report a listing."))
        if (authenticatedReporter != report.reporterId.value) {
            return Result.Error(IllegalStateException("Report identity does not match the signed-in user."))
        }
        val listingId = report.listingId.value.removePrefix(LISTING_ID_PREFIX)
        if (table.hasOpenReport(authenticatedReporter, listingId)) {
            return Result.Error(ListingReportAlreadySubmittedException())
        }
        table.insert(
            ListingReportInsertDto(
                reporterId = authenticatedReporter,
                type = SupabaseListingReportType.LISTING,
                listingId = report.listingId.value.removePrefix(LISTING_ID_PREFIX),
                reason = report.reason.code,
                description = report.description.ifBlank { null }
            )
        )
        Result.Success(Unit)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: PostgrestRestException) {
        Result.Error(
            ListingReportPersistenceFailureMapper.map(exception.code, exception)
        )
    } catch (exception: Exception) {
        Result.Error(IllegalStateException(SAFE_FAILURE_MESSAGE, exception))
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun hasOpenReport(
        reporterId: MarketplaceReporterId,
        listingId: MarketplaceItemId
    ): Result<Boolean> = try {
        val authenticatedReporter = table.authenticatedUserId()
            ?.takeIf(String::isNotBlank)
            ?: return Result.Error(IllegalStateException("Sign in to view reported listings."))
        if (authenticatedReporter != reporterId.value) {
            return Result.Error(IllegalStateException("Report identity does not match the signed-in user."))
        }
        Result.Success(
            table.hasOpenReport(
                authenticatedReporter,
                listingId.value.removePrefix(LISTING_ID_PREFIX)
            )
        )
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.Error(IllegalStateException(SAFE_FAILURE_MESSAGE, exception))
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun reportedListingIds(
        reporterId: MarketplaceReporterId
    ): Result<Set<MarketplaceItemId>> = try {
        val authenticatedReporter = table.authenticatedUserId()
            ?.takeIf(String::isNotBlank)
            ?: return Result.Error(IllegalStateException("Sign in to view reported listings."))
        if (authenticatedReporter != reporterId.value) {
            return Result.Error(IllegalStateException("Report identity does not match the signed-in user."))
        }
        Result.Success(
            table.reportedListingIds(authenticatedReporter)
                .map { "$LISTING_ID_PREFIX$it" }
                .map(::MarketplaceItemId)
                .toSet()
        )
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.Error(IllegalStateException(SAFE_FAILURE_MESSAGE, exception))
    }

    internal companion object {
        const val TABLE = "reports"
        const val SAFE_FAILURE_MESSAGE = "We couldn't submit your report right now. Please try again."
        const val LISTING_ID_PREFIX = "listing:"
        const val UNIQUE_VIOLATION_CODE = "23505"
    }
}

internal object ListingReportPersistenceFailureMapper {
    fun map(code: String?, cause: Exception): IllegalStateException = when (code) {
        UNIQUE_VIOLATION_CODE -> ListingReportAlreadySubmittedException()
        SELF_REPORT_CONSTRAINT_CODE -> ListingReportSelfReportException(cause)
        PERMISSION_DENIED_CODE, INVALID_ENUM_VALUE_CODE ->
            IllegalStateException(SupabaseListingReportRepository.SAFE_FAILURE_MESSAGE, cause)
        else -> IllegalStateException(SupabaseListingReportRepository.SAFE_FAILURE_MESSAGE, cause)
    }

    private const val UNIQUE_VIOLATION_CODE = "23505"
    private const val PERMISSION_DENIED_CODE = "42501"
    private const val INVALID_ENUM_VALUE_CODE = "22P02"
    private const val SELF_REPORT_CONSTRAINT_CODE = "23514"
}

internal interface SupabaseListingReportsTable {
    suspend fun authenticatedUserId(): String?
    suspend fun insert(report: ListingReportInsertDto)
    suspend fun hasOpenReport(reporterId: String, listingId: String): Boolean
    suspend fun reportedListingIds(reporterId: String): Set<String>
}

internal class PostgrestSupabaseListingReportsTable(
    private val supabase: SupabaseClient
) : SupabaseListingReportsTable {
    override suspend fun authenticatedUserId(): String? {
        supabase.auth.awaitInitialization()
        return supabase.auth.currentUserOrNull()?.id
    }

    override suspend fun insert(report: ListingReportInsertDto) {
        supabase.from(SupabaseListingReportRepository.TABLE).insert(report)
    }

    override suspend fun hasOpenReport(reporterId: String, listingId: String): Boolean =
        supabase.from(SupabaseListingReportRepository.TABLE)
            .select(columns = Columns.raw("status")) {
                filter {
                    eq("reporter_id", reporterId)
                    eq("type", "listing")
                    eq("listing_id", listingId)
                }
            }
            .decodeList<ListingReportStatusDto>()
            .any { it.status.isOpenListingReportStatus() }

    override suspend fun reportedListingIds(reporterId: String): Set<String> =
        supabase.from(SupabaseListingReportRepository.TABLE)
            .select(columns = Columns.raw("listing_id,status")) {
                filter {
                    eq("reporter_id", reporterId)
                    eq("type", "listing")
                }
            }
            .decodeList<ListingReportListingIdDto>()
            .filter { it.status.isOpenListingReportStatus() }
            .map(ListingReportListingIdDto::listingId)
            .toSet()
}

internal fun String?.isOpenListingReportStatus(): Boolean = ReportStatus.fromDatabaseValue(this).isOpen
