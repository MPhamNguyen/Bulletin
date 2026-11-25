package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.application.ManageListingImages
import com.jdrms.bulletin.domain.listings.domain.model.Listing

/** Photo updates follow the details save; a failure here must not roll back saved details. */
internal suspend fun synchronizeEditedListingImages(
    draft: ValidatedEditDraft,
    seller: ListingSeller,
    listing: Listing,
    manageImages: ManageListingImages?,
    manageListing: ManageListing
): Result<Listing> {
    val retained = draft.photos.filterIsInstance<ListingPhotoDraft.Existing>().map { it.url }
    val uploads = draft.photos.filterIsInstance<ListingPhotoDraft.Pending>().map { it.upload }
    if (draft.currentListing.images.isEmpty() && uploads.isEmpty()) return Result.Success(listing)
    val result = manageImages?.synchronize(listing.id, seller.id, retained, uploads)
        ?: Result.Error(IllegalStateException("Listing image updates are not configured."))
    return when (result) {
        is Result.Success -> manageListing.updateListing(listing.copy(images = result.data), seller.id)
        is Result.Error -> result
    }
}
