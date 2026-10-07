package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.profile.application.ProfileSoldListingsProvider
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.CancellationException

class ListingsSoldListingsCountProvider(
    private val listingsRepository: ListingsRepository
) : ProfileSoldListingsProvider {
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun getSoldListingsCount(userId: UserId): Int {
        return try {
            listingsRepository.getSellerListings(SellerId(userId.value))
                .count { it.status == ListingStatus.SOLD }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            0
        }
    }
}
