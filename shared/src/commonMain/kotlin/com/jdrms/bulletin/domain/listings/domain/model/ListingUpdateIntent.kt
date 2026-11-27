package com.jdrms.bulletin.domain.listings.domain.model

/**
 * Why a listing row is being written.
 *
 * `edited_at` is owned by the server and means "the seller last intentionally changed this listing". A writer
 * says so with [EDIT]; every other write leaves the timestamp as it was.
 */
enum class ListingUpdateIntent {
    /** The seller intentionally changed the listing (edit form, sold, restored). Advances `edited_at`. */
    EDIT,

    /** Bookkeeping such as syncing image rows after creation. Leaves `edited_at` unchanged. */
    ROUTINE
}
