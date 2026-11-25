package com.jdrms.bulletin.domain.listings

import com.jdrms.bulletin.domain.listings.attributes.attributeListing
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingUpdateIntent
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingDto
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingInsertDto
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingWriteDto
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.SupabaseListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.SupabaseListingsTable
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

private class StoredRow(val dto: SupabaseListingWriteDto, val createdAt: Long, val editedAt: Long?)

/**
 * Every [ListingsRepository] must agree on `edited_at`: only an explicit edit advances it, callers can never
 * supply it, and `created_at` never moves. The Supabase side emulates the hosted `edit_listing` RPC.
 */
class ListingEditedAtContractTest {
    private var now = CREATED_AT

    /** Emulates the RPC from `20261125010000_listing_edit_rpc.sql`. */
    private inner class RpcEmulatingTable : SupabaseListingsTable {
        private val rows = linkedMapOf<String, StoredRow>()

        override suspend fun findById(id: String) = rows[id]?.toReadDto()

        override suspend fun insert(listing: SupabaseListingInsertDto) {
            rows[listing.id] = StoredRow(listing, createdAt = now, editedAt = null)
        }

        override suspend fun update(listing: SupabaseListingWriteDto, intent: ListingUpdateIntent) {
            val row = rows.getValue(listing.id)
            val editedAt = if (intent == ListingUpdateIntent.EDIT) {
                maxOf(
                    now,
                    row.editedAt ?: Long.MIN_VALUE
                )
            } else {
                row.editedAt
            }
            rows[listing.id] = StoredRow(listing, row.createdAt, editedAt)
        }

        override suspend fun delete(id: String, sellerId: String) {
            rows.remove(id)
        }

        override suspend fun getListings(sellerId: String?) =
            rows.values.filter { sellerId == null || it.dto.userId == sellerId }.map { it.toReadDto() }

        private fun StoredRow.toReadDto() = SupabaseListingDto(
            id = dto.id, name = dto.name, userId = dto.userId, category = dto.category,
            condition = dto.condition, price = dto.price, description = dto.description,
            createdAt = Instant.fromEpochMilliseconds(createdAt).toString(),
            editedAt = editedAt?.let { Instant.fromEpochMilliseconds(it).toString() },
            status = dto.status, buyerId = dto.buyerId, attributes = dto.attributes
        )
    }

    private fun implementations(): Map<String, ListingsRepository> = mapOf(
        "in-memory" to InMemoryListingsRepository(emptyList()) { now },
        "supabase" to SupabaseListingsRepository(RpcEmulatingTable()) { }
    )

    private fun Listing.isEdited() = editedAtMillis != null

    private suspend fun ListingsRepository.stored(): Listing = getListing(ListingId("listing-1"))!!

    private suspend fun ListingsRepository.seed() {
        now = CREATED_AT
        createListing(attributeListing().copy(createdAtMillis = CREATED_AT))
    }

    @Test
    fun aNewListingIsNotEdited() = runTest {
        implementations().forEach { (name, repository) ->
            repository.seed()
            assertFalse(repository.stored().isEdited(), name)
        }
    }

    @Test
    fun routineUpdatesNeverAdvanceEditedAtAndIgnoreCallerTimestamps() = runTest {
        implementations().forEach { (name, repository) ->
            repository.seed()
            val before = repository.stored()
            now = CREATED_AT + 1_000

            repository.updateListing(
                before.copy(description = "Routine", createdAtMillis = 5L, editedAtMillis = 99_999L),
                ListingUpdateIntent.ROUTINE
            )
            repository.updateListing(before.copy(description = "Default intent"))

            val after = repository.stored()
            assertEquals(before.editedAtMillis, after.editedAtMillis, name)
            assertEquals(before.createdAtMillis, after.createdAtMillis, name)
            assertEquals("Default intent", after.description, name)
            assertFalse(after.isEdited(), name)
        }
    }

    @Test
    fun anExplicitEditAdvancesEditedAtToServerTimeAndKeepsCreatedAt() = runTest {
        implementations().forEach { (name, repository) ->
            repository.seed()
            now = CREATED_AT + 2_000

            repository.updateListing(
                repository.stored().copy(title = "Edited title", editedAtMillis = 99_999L),
                ListingUpdateIntent.EDIT
            )

            val after = repository.stored()
            assertEquals(CREATED_AT + 2_000, after.editedAtMillis, name)
            assertEquals(CREATED_AT, after.createdAtMillis, name)
            assertTrue(after.isEdited(), name)
        }
    }

    @Test
    fun anUnchangedEditSaveStillAdvancesEditedAt() = runTest {
        implementations().forEach { (name, repository) ->
            repository.seed()
            now = CREATED_AT + 3_000

            repository.updateListing(repository.stored(), ListingUpdateIntent.EDIT)

            assertEquals(CREATED_AT + 3_000, repository.stored().editedAtMillis, name)
        }
    }

    @Test
    fun routineWritesAfterAnEditKeepTheEditTime() = runTest {
        implementations().forEach { (name, repository) ->
            repository.seed()
            now = CREATED_AT + 4_000
            repository.updateListing(repository.stored(), ListingUpdateIntent.EDIT)
            now = CREATED_AT + 5_000

            repository.updateListing(repository.stored().copy(images = listOf("a.jpg")), ListingUpdateIntent.ROUTINE)

            assertEquals(CREATED_AT + 4_000, repository.stored().editedAtMillis, name)
        }
    }

    private companion object {
        const val CREATED_AT = 1_000_000L
    }
}
