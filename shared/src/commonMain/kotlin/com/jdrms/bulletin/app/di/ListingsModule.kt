package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.app.integration.AuthListingSellerProvider
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.DeleteListing
import com.jdrms.bulletin.domain.listings.application.GetSellerListings
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.application.ManageListingImages
import com.jdrms.bulletin.domain.listings.application.MarkListingSold
import com.jdrms.bulletin.domain.listings.application.RestoreListingToMarketplace
import com.jdrms.bulletin.domain.listings.domain.repository.ListingImagesRepository
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingImagesRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.SupabaseListingImagesRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.SupabaseListingsRepository

internal class ListingsModule(
    private val infrastructure: InfrastructureModule,
    private val profile: ProfileModule,
    private val afterSuccessfulMutation: suspend () -> Unit
) {
    internal val listingsRepository: ListingsRepository by lazy {
        infrastructure.backend(
            supabase = { client -> SupabaseListingsRepository(client, afterSuccessfulMutation) },
            inMemory = { InMemoryListingsRepository() }
        )
    }
    internal val listingImagesRepository: ListingImagesRepository by lazy {
        infrastructure.backend(
            supabase = ::SupabaseListingImagesRepository,
            inMemory = ::InMemoryListingImagesRepository
        )
    }
    val createListing by lazy { CreateListing(listingsRepository) }
    val manageListing by lazy { ManageListing(listingsRepository) }
    val manageListingImages by lazy { ManageListingImages(listingImagesRepository) }
    val markListingSold by lazy { MarkListingSold(listingsRepository) }
    val restoreListingToMarketplace by lazy { RestoreListingToMarketplace(listingsRepository) }
    val deleteListing by lazy { DeleteListing(listingsRepository) }
    val getSellerListings by lazy { GetSellerListings(listingsRepository) }
    val currentListingSellerProvider by lazy { AuthListingSellerProvider(profile.authRepository) }
}
