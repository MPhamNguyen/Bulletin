package com.jdrms.bulletin.domain.listings.domain.service

/** New listing drafts must include a photo before they can be submitted. */
object ListingPhotoPolicy {
    const val REQUIRED_PHOTO_MESSAGE = "Add at least one photo to post your listing."

    fun hasRequiredPhoto(imageCount: Int): Boolean = imageCount >= 1
}
