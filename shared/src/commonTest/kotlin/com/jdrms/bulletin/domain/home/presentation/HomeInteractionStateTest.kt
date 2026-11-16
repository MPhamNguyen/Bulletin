package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.domain.home.domain.model.CardAction
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeInteractionStateTest {
    @Test
    fun account_notice_does_not_determine_authenticated_actions() {
        val signedInWithNotice = HomeInteractionState(
            listingId = HomeItemId("listing"),
            accountNotice = HomeInteractionError.NOT_SIGNED_IN,
            signedIn = true,
            saveAvailable = true,
            messageAvailable = true
        )
        val signedOutWithoutNotice = signedInWithNotice.copy(signedIn = false, accountNotice = null)

        assertTrue(signedInWithNotice.availableActions.allows(CardAction.Save))
        assertTrue(signedInWithNotice.availableActions.allows(CardAction.Message))
        assertFalse(signedOutWithoutNotice.availableActions.allows(CardAction.Save))
        assertFalse(signedOutWithoutNotice.availableActions.allows(CardAction.Message))
    }
}
