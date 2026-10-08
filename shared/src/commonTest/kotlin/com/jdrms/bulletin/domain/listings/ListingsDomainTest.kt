package com.jdrms.bulletin.domain.listings

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.DeleteListing
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.service.ListingValidationException
import com.jdrms.bulletin.domain.listings.domain.service.ListingValidationPolicy
import com.jdrms.bulletin.domain.listings.infrastructure.dto.ListingDto
import com.jdrms.bulletin.domain.listings.infrastructure.mapper.ListingMapper
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListingsDomainTest {

    private val policy = ListingValidationPolicy()

    @Test
    fun testValidPriceFormatting() {
        assertEquals("$40.00", ListingPrice(40.0).formatted)
        assertEquals("$19.99", ListingPrice(19.99).formatted)
        assertEquals("$25.50", ListingPrice(25.5).formatted)
        assertEquals("$1,250.00", ListingPrice(1250.0).formatted)
    }

    @Test
    fun testNegativePriceThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            ListingPrice(-10.0)
        }
    }

    @Test
    fun testPriceWithMoreThanTwoDecimalPlacesThrowsException() {
        val exception = assertFailsWith<IllegalArgumentException> {
            ListingPrice(40.555)
        }

        assertEquals("Listing price cannot have more than two decimal places.", exception.message)
    }

    @Test
    fun testPriceAboveMarketplaceLimitThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            ListingPrice(10_000_000.0)
        }
    }

    @Test
    fun testShortTitleThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            Listing(
                id = ListingId("list_short"),
                sellerId = SellerId("s1"),
                sellerName = "Seller",
                title = "No",
                description = "Desc",
                price = ListingPrice(10.0),
                category = ListingCategory.OTHER
            )
        }
    }

    @Test
    fun testListingValidationPolicy() {
        val validListing = Listing(
            id = ListingId("list_valid"),
            sellerId = SellerId("s1"),
            sellerName = "Dominic",
            title = "CECS 491 Project Guide",
            description = "Complete reference manual",
            price = ListingPrice(15.0),
            category = ListingCategory.TEXTBOOKS
        )
        val result = policy.validateListing(validListing)
        assertTrue(result.isSuccess())
    }

    @Test
    fun testListingOwnershipCheck() {
        val listing = testListing(SellerId("owner_1"))
        assertTrue(listing.isOwnedBy(SellerId("owner_1")))
        assertFalse(listing.isOwnedBy(SellerId("other_user")))
    }

    @Test
    fun ownerCanMarkAvailableListingSold() {
        val listing = testListing(SellerId("owner_1"))

        val sold = listing.markSold(SellerId("owner_1"))

        assertEquals(ListingStatus.SOLD, sold.status)
        assertEquals(listing.title, sold.title)
    }

    @Test
    fun nonOwnerCannotMarkListingSold() {
        val exception = assertFailsWith<IllegalArgumentException> {
            testListing(SellerId("owner_1")).markSold(SellerId("stranger"))
        }

        assertEquals("Only the owner can mark this listing as sold.", exception.message)
    }

    @Test
    fun soldListingCannotBeMarkedSoldAgain() {
        val soldListing = testListing(SellerId("owner_1")).markSold(SellerId("owner_1"))

        val exception = assertFailsWith<IllegalArgumentException> {
            soldListing.markSold(SellerId("owner_1"))
        }

        assertEquals("Only available listings can be marked as sold.", exception.message)
    }

    @Test
    fun ownerCanRestoreSoldListingToMarketplace() {
        val soldListing = testListing(SellerId("owner_1")).markSold(SellerId("owner_1"))

        val restored = soldListing.restoreToMarketplace(SellerId("owner_1"))

        assertEquals(ListingStatus.AVAILABLE, restored.status)
    }

    @Test
    fun onlyOwnerCanRestoreSoldListingToMarketplace() {
        val soldListing = testListing(SellerId("owner_1")).markSold(SellerId("owner_1"))

        val exception = assertFailsWith<IllegalArgumentException> {
            soldListing.restoreToMarketplace(SellerId("stranger"))
        }

        assertEquals("Only the owner can restore this listing.", exception.message)
    }

    @Test
    fun availableListingCannotBeRestoredToMarketplace() {
        val exception = assertFailsWith<IllegalArgumentException> {
            testListing(SellerId("owner_1")).restoreToMarketplace(SellerId("owner_1"))
        }

        assertEquals("Only sold listings can be restored to the marketplace.", exception.message)
    }

    @Test
    fun markListingSoldUseCaseRequiresOwnershipAndPersistsTransition() = runTest {
        val repository = InMemoryListingsRepository(initialListings = emptyList())
        val listing = testListing(SellerId("owner_1"))
        repository.createListing(listing)
        val markListingSold = com.jdrms.bulletin.domain.listings.application.MarkListingSold(repository, policy)

        val unauthorized = markListingSold(listing.id, SellerId("stranger"))
        assertTrue(unauthorized.isError())
        assertEquals(ListingStatus.AVAILABLE, repository.getListing(listing.id)?.status)

        val success = markListingSold(listing.id, SellerId("owner_1"))
        assertTrue(success.isSuccess())
        assertEquals(ListingStatus.SOLD, repository.getListing(listing.id)?.status)
    }

    @Test
    fun testOwnerCanUpdateListingDetailsAndImages() {
        val listing = testListing(SellerId("owner_1"))
        val updated = listing.updateDetails(
            editorSellerId = SellerId("owner_1"),
            title = "Updated Keyboard Title",
            description = "Updated mechanical switches description",
            price = ListingPrice(45.0),
            category = ListingCategory.ELECTRONICS,
            condition = ListingCondition.GOOD,
            images = listOf("https://example.com/keyboard.jpg")
        )

        assertEquals("Updated Keyboard Title", updated.title)
        assertEquals("Updated mechanical switches description", updated.description)
        assertEquals(45.0, updated.price.amount)
        assertEquals(listOf("https://example.com/keyboard.jpg"), updated.images)
    }

    @Test
    fun testNonOwnerCannotUpdateListingDetailsThrows() {
        val listing = testListing(SellerId("owner_1"))
        val exception = assertFailsWith<IllegalArgumentException> {
            listing.updateDetails(
                editorSellerId = SellerId("stranger"),
                title = "Hacked Title"
            )
        }
        assertEquals("Only the owner can edit this listing.", exception.message)
    }

    @Test
    fun testListingValidationPolicyOwnership() {
        val listing = testListing(SellerId("owner_1"))
        val success = policy.validateOwnership(listing, SellerId("owner_1"))
        assertTrue(success.isSuccess())

        val failure = policy.validateOwnership(listing, SellerId("non_owner"))
        assertTrue(failure.isError())
        assertTrue((failure as Result.Error).exception is ListingValidationException.Unauthorized)
    }

    @Test
    fun testManageListingUpdateWithOwnershipEnforcement() = runTest {
        val repo = InMemoryListingsRepository(initialListings = emptyList())
        val manageListing = ManageListing(repo, policy)
        val listing = testListing(SellerId("owner_1"))
        repo.createListing(listing)

        val nonOwnerResult = manageListing.updateListing(
            listing = listing.copy(title = "Changed by stranger"),
            editorSellerId = SellerId("stranger")
        )
        assertTrue(nonOwnerResult.isError())
        assertTrue((nonOwnerResult as Result.Error).exception is ListingValidationException.Unauthorized)

        val ownerResult = manageListing.updateListing(
            listing = listing.copy(title = "Changed by owner", price = ListingPrice(50.0)),
            editorSellerId = SellerId("owner_1")
        )
        assertTrue(ownerResult.isSuccess())
        val updated = repo.getSellerListings(SellerId("owner_1")).first()
        assertEquals("Changed by owner", updated.title)
        assertEquals(50.0, updated.price.amount)
    }

    @Test
    fun testManageListingRejectsForgedSellerPayload() = runTest {
        val repo = InMemoryListingsRepository(initialListings = emptyList())
        val manageListing = ManageListing(repo, policy)
        val persisted = testListing(SellerId("owner_1"))
        repo.createListing(persisted)

        val forgedPayload = persisted.copy(
            sellerId = SellerId("attacker"),
            title = "Stolen Listing"
        )
        val result = manageListing.updateListing(forgedPayload, SellerId("attacker"))

        assertTrue(result.isError())
        assertTrue((result as Result.Error).exception is ListingValidationException.Unauthorized)
        val stored = repo.getListing(persisted.id)
        assertEquals(SellerId("owner_1"), stored?.sellerId)
        assertEquals(persisted.title, stored?.title)
    }

    @Test
    fun testCreateAndManageListingInRepository() = runTest {
        val repo = InMemoryListingsRepository(initialListings = emptyList())
        val newListing = Listing(
            id = ListingId("list_test_1"),
            sellerId = SellerId("seller_99"),
            sellerName = "Tester",
            title = "Wireless Keyboard",
            description = "Mechanical switches, quiet typing",
            price = ListingPrice(35.0),
            category = ListingCategory.ELECTRONICS,
            condition = ListingCondition.LIKE_NEW
        )

        val createResult = repo.createListing(newListing)
        assertTrue(createResult.isSuccess())

        val sellerListings = repo.getSellerListings(SellerId("seller_99"))
        assertEquals(1, sellerListings.size)
        assertEquals("Wireless Keyboard", sellerListings.first().title)

        val deleteResult = repo.deleteListing(ListingId("list_test_1"), SellerId("seller_99"))
        assertTrue(deleteResult.isSuccess())
        assertEquals(0, repo.getSellerListings(SellerId("seller_99")).size)
    }

    @Test
    fun testDeleteListingRequiresOwnership() = runTest {
        val repo = InMemoryListingsRepository(initialListings = emptyList())
        val listing = testListing(SellerId("owner_1"))
        repo.createListing(listing)
        val deleteListing = DeleteListing(repo, policy)

        val result = deleteListing(listing.id, SellerId("stranger"))

        assertTrue(result.isError())
        assertEquals("Only the owner can delete this listing.", (result as Result.Error).message)
        assertEquals(listing, repo.getListing(listing.id))
    }

    @Test
    fun testOwnerCanDeleteListing() = runTest {
        val repo = InMemoryListingsRepository(initialListings = emptyList())
        val listing = testListing(SellerId("owner_1"))
        repo.createListing(listing)
        val deleteListing = DeleteListing(repo, policy)

        val result = deleteListing(listing.id, SellerId("owner_1"))

        assertTrue(result.isSuccess())
        assertEquals(null, repo.getListing(listing.id))
    }

    @Test
    fun testMapperRoundTrip() {
        val dto = ListingDto(
            id = "dto_list_1",
            sellerId = "s_1",
            sellerName = "Dominic",
            title = "Desk Chair",
            description = "Mesh back",
            price = 45.0,
            category = "FURNITURE",
            condition = "LIKE_NEW",
            status = "AVAILABLE",
            images = listOf("https://example.com/chair1.jpg", "https://example.com/chair2.jpg")
        )
        val domain = ListingMapper.toDomain(dto)
        assertEquals(ListingCategory.FURNITURE, domain.category)
        assertEquals(ListingCondition.LIKE_NEW, domain.condition)
        assertEquals(ListingStatus.AVAILABLE, domain.status)
        assertEquals(listOf("https://example.com/chair1.jpg", "https://example.com/chair2.jpg"), domain.images)

        val backToDto = ListingMapper.toDto(domain)
        assertEquals(dto.id, backToDto.id)
        assertEquals(dto.condition, backToDto.condition)
        assertEquals(dto.images, backToDto.images)
    }

    private fun testListing(sellerId: SellerId): Listing {
        return Listing(
            id = ListingId("list_123"),
            sellerId = sellerId,
            sellerName = "Student Seller",
            title = "Mechanical Keyboard",
            description = "Quiet tactile switches",
            price = ListingPrice(40.0),
            category = ListingCategory.ELECTRONICS,
            condition = ListingCondition.GOOD,
            status = ListingStatus.AVAILABLE
        )
    }
}
