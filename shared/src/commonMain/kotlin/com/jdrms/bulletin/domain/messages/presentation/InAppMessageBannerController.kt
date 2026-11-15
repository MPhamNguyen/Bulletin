package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.application.IncomingMessageAlert
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Owns the banner that is visible. It lives with the app session rather than a composition, so rotation and
 * recomposition neither re-show a dismissed banner nor lose a visible one.
 *
 * Alerts are dropped, not held, while the screen is not eligible. Alerts addressed to anyone but the current
 * viewer are dropped, and a sign-out or account switch clears everything, so one account's text never shows
 * for another.
 */
internal class InAppMessageBannerController(
    private val inbox: StateFlow<InboxState>,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val queue = InAppBannerQueue()
    private val mutableVisible = MutableStateFlow<IncomingMessageAlert?>(null)
    private var isEligible = false
    private var job: Job? = null

    val visible: StateFlow<IncomingMessageAlert?> = mutableVisible.asStateFlow()

    /** Idempotent. */
    fun start(alerts: Flow<IncomingMessageAlert>) {
        if (job?.isActive == true) return
        job = scope.launch {
            launch { inbox.map { it.viewerId }.distinctUntilChanged().collect { clear() } }
            launch { alerts.collect(::offer) }
        }
    }

    /** Becoming ineligible discards what is queued; those alerts are not shown later. */
    fun setEligible(eligible: Boolean) {
        if (isEligible == eligible) return
        isEligible = eligible
        if (!eligible) clear()
    }

    fun dismiss(messageId: MessageId) {
        if (queue.dismiss(messageId)) publish()
    }

    fun close() {
        scope.cancel()
        job = null
        clear()
    }

    private fun offer(alert: IncomingMessageAlert) {
        if (!isEligible || alert.recipientId != inbox.value.viewerId) return
        queue.offer(alert)
        publish()
    }

    private fun clear() {
        queue.clear()
        publish()
    }

    private fun publish() {
        mutableVisible.value = queue.head
    }
}
