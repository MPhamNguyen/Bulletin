package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestPreferenceException
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterest
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.MarketplaceInterestDto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class SupabaseMarketplaceInterestPreferenceStoreTest {
    @Test
    fun readsAndMapsTheAuthenticatedOwnersInterests() = runTest {
        var fetchedOwnerId: String? = null
        val store = store(
            fetch = { ownerId ->
                fetchedOwnerId = ownerId
                listOf(MarketplaceInterestDto("TEXTBOOKS"), MarketplaceInterestDto("ELECTRONICS"))
            }
        )

        val interests = store.get(ownerId)

        assertEquals(ownerId.value, fetchedOwnerId)
        assertEquals(
            setOf(
                MarketplaceInterest(MarketplaceCategory.TEXTBOOKS),
                MarketplaceInterest(MarketplaceCategory.ELECTRONICS)
            ),
            interests
        )
    }

    @Test
    fun replacesInterestsWithCanonicalCategoryNames() = runTest {
        var replacedInterests = emptyList<MarketplaceInterestDto>()
        val store = store(replace = { replacedInterests = it })

        store.set(
            ownerId,
            setOf(
                MarketplaceInterest(MarketplaceCategory.HOUSING),
                MarketplaceInterest(MarketplaceCategory.CLOTHING, MarketplaceCondition.GOOD)
            )
        )

        assertEquals(setOf("HOUSING", "CLOTHING"), replacedInterests.map { it.category }.toSet())
        assertEquals("GOOD", replacedInterests.single { it.category == "CLOTHING" }.condition)
    }

    @Test
    fun rejectsAnOwnerThatDoesNotMatchTheAuthenticatedUser() = runTest {
        var fetched = false
        var replaced = false
        val store = SupabaseMarketplaceInterestPreferenceStore(
            authenticatedUserId = { "different-user" },
            fetch = {
                fetched = true
                emptyList()
            },
            replace = {
                replaced = true
            }
        )

        assertFailsWith<IllegalStateException> { store.get(ownerId) }
        assertFailsWith<IllegalStateException> { store.set(ownerId, emptySet()) }
        assertFalse(fetched)
        assertFalse(replaced)
    }

    @Test
    fun translatesBackendWriteFailures() = runTest {
        val cause = IllegalStateException("RPC unavailable")
        val store = store(replace = { throw cause })

        val failure = assertFailsWith<MarketplaceInterestPreferenceException> {
            store.set(ownerId, setOf(MarketplaceInterest(MarketplaceCategory.OTHER)))
        }

        assertEquals("Unable to save marketplace interests.", failure.message)
        assertEquals(cause, failure.cause)
    }

    private fun store(
        fetch: suspend (String) -> List<MarketplaceInterestDto> = { emptyList() },
        replace: suspend (List<MarketplaceInterestDto>) -> Unit = {}
    ) = SupabaseMarketplaceInterestPreferenceStore(
        authenticatedUserId = { ownerId.value },
        fetch = fetch,
        replace = replace
    )

    private companion object {
        val ownerId = MarketplaceInterestOwnerId("student-one")
    }
}
