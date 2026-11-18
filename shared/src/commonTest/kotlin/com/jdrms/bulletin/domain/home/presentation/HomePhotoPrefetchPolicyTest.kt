package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import kotlin.test.Test
import kotlin.test.assertEquals

class HomePhotoPrefetchPolicyTest {
    private fun item(id: String, vararg photos: String, imageUrl: String? = null) =
        homeItem(id).let { base ->
            HomeFeedItem(
                base.id,
                base.title,
                base.description,
                base.price,
                base.category,
                imageUrl = imageUrl,
                photoUrls = photos.toList()
            )
        }

    @Test
    fun ordersCurrentThenNextThenFollowing() {
        val feed = listOf(item("a", "a1", "a2"), item("b", "b1"), item("c", "c1"))
        assertEquals(listOf("a1", "a2", "b1", "c1"), homePhotoPrefetchUrls(feed, 0))
        assertEquals(listOf("b1", "c1"), homePhotoPrefetchUrls(feed, 1))
    }

    @Test
    fun respectsLimit() {
        val feed = listOf(item("a", "a1", "a2", "a3"), item("b", "b1", "b2"))
        assertEquals(listOf("a1", "a2", "a3", "b1"), homePhotoPrefetchUrls(feed, 0, limit = 4))
        assertEquals(emptyList(), homePhotoPrefetchUrls(feed, 0, limit = 0))
    }

    @Test
    fun skipsItemsWithoutPhotosAndFallsBackToImageUrl() {
        val feed = listOf(item("a"), item("b", imageUrl = "b-legacy"), item("c", "c1"))
        assertEquals(listOf("b-legacy", "c1"), homePhotoPrefetchUrls(feed, 0))
    }

    @Test
    fun handlesEndOfFeedAndOutOfRangeIndex() {
        val feed = listOf(item("a", "a1"), item("b", "b1"))
        assertEquals(listOf("b1"), homePhotoPrefetchUrls(feed, 1))
        assertEquals(emptyList(), homePhotoPrefetchUrls(feed, 2))
        assertEquals(emptyList(), homePhotoPrefetchUrls(feed, -1))
        assertEquals(emptyList(), homePhotoPrefetchUrls(emptyList(), 0))
    }

    @Test
    fun removesDuplicatesIncludingCropVariants() {
        val feed = listOf(
            item("a", "u1#bulletin_crop=1.5,0.1,0.2", "u1"),
            item("b", "u1#bulletin_crop=2.0,0.0,0.0", "u2")
        )
        assertEquals(listOf("u1", "u2"), homePhotoPrefetchUrls(feed, 0))
    }

    @Test
    fun stripsCropFragmentFromRequestUrls() {
        val feed = listOf(item("a", "https://x/y.jpg#bulletin_crop=1.2,0.0,0.0"))
        assertEquals(listOf("https://x/y.jpg"), homePhotoPrefetchUrls(feed, 0))
    }
}
