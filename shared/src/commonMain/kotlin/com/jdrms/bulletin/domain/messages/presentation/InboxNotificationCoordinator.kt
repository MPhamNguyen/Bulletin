package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.application.IncomingMessageAlert
import com.jdrms.bulletin.domain.messages.application.IncomingMessageNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Observes the single session inbox and routes each new incoming message by app state: while the app is
 * foregrounded the alert is published on [alerts] for the in-app banner, and while backgrounded it goes to the
 * platform notifier. An alert is never delivered to both. It reads the shared inbox state; it does not own a
 * second inbox or subscription.
 */
class InboxNotificationCoordinator(
    private val inbox: StateFlow<InboxState>,
    private val activeConversation: ActiveConversationTracker,
    private val notifier: IncomingMessageNotifier,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val detector = IncomingMessageAlertDetector()
    private val mutableAlerts = MutableSharedFlow<IncomingMessageAlert>(
        extraBufferCapacity = ALERT_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Foreground alerts, with no replay: a collector sees only alerts raised after it subscribes. */
    val alerts: SharedFlow<IncomingMessageAlert> = mutableAlerts.asSharedFlow()
    private var job: Job? = null

    /** Idempotent. */
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            combine(inbox, activeConversation.suppressedConversation) { state, suppressed ->
                detector.detect(state, suppressed)
            }.collect { detection ->
                if (detection.clearAll) notifier.clearAll()
                detection.cleared.forEach(notifier::clear)
                detection.alerts.forEach(::route)
            }
        }
    }

    fun close() {
        scope.cancel()
        job = null
    }

    private fun route(alert: IncomingMessageAlert) {
        if (activeConversation.isForeground.value) mutableAlerts.tryEmit(alert) else notifier.show(alert)
    }

    private companion object {
        const val ALERT_BUFFER = 8
    }
}
