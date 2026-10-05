package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.profile.application.ProfileActiveListingsProvider
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.CancellationException

class ListingsActiveListingsCountProvider(
    private val listingsRepository: ListingsRepository
) : ProfileActiveListingsProvider {
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun getActiveListingsCount(userId: UserId): Int {
        return try {
            listingsRepository.getSellerListings(SellerId(userId.value))
                .count { it.status == ListingStatus.AVAILABLE }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            0
        }
    }
}
