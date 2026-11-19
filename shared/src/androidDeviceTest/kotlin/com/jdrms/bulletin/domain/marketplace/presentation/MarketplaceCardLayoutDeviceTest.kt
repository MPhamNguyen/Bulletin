package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePrice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class MarketplaceCardLayoutDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun compactLightCards() = verifyCards(false, 1f)

    @Test fun largeTextDarkCards() = verifyCards(true, 2f)

    private fun verifyCards(dark: Boolean, fontScale: Float) {
        var saves = 0
        var opens = 0
        val short = item("Book", 20.0)
        val long = item("A very long listing title that must stay on one line", 999999.99)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                BulletinTheme(darkTheme = dark) {
                    Row(Modifier.width(296.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(short, long).forEachIndexed { index, listing ->
                            Column(Modifier.weight(1f)) {
                                MarketplaceItemCard(
                                    item = listing,
                                    isBookmarked = index == 1,
                                    onBookmarkClick = { saves++ },
                                    onCardClick = { opens++ }
                                )
                            }
                        }
                    }
                }
            }
        }
        assertAlignedRows(short, long)
        compose.onNodeWithContentDescription("Bookmark listing").performClick()
        compose.onNodeWithContentDescription("Remove bookmark").performClick()
        compose.runOnIdle {
            assertEquals(2, saves)
            assertEquals(0, opens)
        }
        compose.onNodeWithText(short.title).performClick()
        compose.runOnIdle { assertEquals(1, opens) }
    }

    private fun assertAlignedRows(short: MarketplaceItem, long: MarketplaceItem) {
        val titles = listOf(short, long).map { compose.onNodeWithText(it.title).fetchSemanticsNode().boundsInRoot }
        val prices = listOf(short, long).map {
            compose.onNodeWithText(it.price.formatted).fetchSemanticsNode().boundsInRoot
        }
        assertEquals(titles[0].top, titles[1].top)
        assertEquals(titles[0].height, titles[1].height)
        assertEquals(prices[0].top, prices[1].top)
        assertEquals(prices[0].height, prices[1].height)
    }

    private fun item(title: String, price: Double) = MarketplaceItem(
        id = MarketplaceItemId(title),
        sellerId = "seller",
        sellerName = "A seller with an exceptionally long display name",
        title = title,
        description = "Description should not expand the compact card",
        price = MarketplacePrice(price),
        category = MarketplaceCategory.OTHER
    )
}
