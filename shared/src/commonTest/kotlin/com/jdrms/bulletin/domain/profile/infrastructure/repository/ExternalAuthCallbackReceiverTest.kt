package com.jdrms.bulletin.domain.profile.infrastructure.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExternalAuthCallbackReceiverTest {
    @Test
    fun acceptsOnlyExactCallbackForPendingRequestAndConsumesItOnce() {
        var started: Long? = 1_000L
        val receiver = ExternalAuthCallbackReceiver({ started }, { started = null }, { 2_000L })
        val callback = "${MicrosoftOAuthRedirect.URI}?code=one-time-code"
        for (uri in listOf(
            "other://oauth/callback?code=a",
            "${MicrosoftOAuthRedirect.URI}/extra?code=a",
            "com.jdrms.bulletin.auth://attacker@oauth/callback?code=a",
            "$callback#access_token=bad"
        )) {
            assertNull(receiver.receive(uri))
            assertEquals(1_000L, started)
        }
        assertEquals(ExternalAuthCallback.Code("one-time-code"), receiver.receive(callback))
        assertNull(receiver.receive(callback))
    }

    @Test
    fun unsolicitedExpiredAndFutureCallbacksNeverExchangeCodes() {
        val callback = "${MicrosoftOAuthRedirect.URI}?code=one-time-code"
        assertNull(ExternalAuthCallbackReceiver({ null }, {}, { 0L }).receive(callback))
        for (now in listOf(-1L, ExternalAuthCallbackReceiver.LIFETIME_MS + 1)) {
            assertEquals(
                ExternalAuthCallback.Rejected("expired"),
                ExternalAuthCallbackReceiver({ 0L }, {}, { now }).receive(callback)
            )
        }
    }

    @Test
    fun duplicateOrAmbiguousParametersAreRejectedAndCancellationIsSeparate() {
        for (query in listOf("code=a&code=b", "code=", "code=a&error=access_denied", "error=unknown")) {
            assertEquals(
                ExternalAuthCallback.Rejected(null),
                ExternalAuthCallbackReceiver({ 0L }, {}, { 1L })
                    .receive("${MicrosoftOAuthRedirect.URI}?$query")
            )
        }
        assertEquals(
            ExternalAuthCallback.Cancelled,
            ExternalAuthCallbackReceiver({ 0L }, {}, { 1L })
                .receive("${MicrosoftOAuthRedirect.URI}?error=access_denied")
        )
    }

    @Test
    fun mapsOnlyFixedServerRejectionsWithoutReturningProviderText() {
        val callback = "${MicrosoftOAuthRedirect.URI}?error=server_error&error_description=bulletin_account_conflict"
        assertEquals(
            ExternalAuthCallback.Rejected("bulletin_account_conflict"),
            ExternalAuthCallbackReceiver({ 0L }, {}, { 1L }).receive(callback)
        )
    }
}
