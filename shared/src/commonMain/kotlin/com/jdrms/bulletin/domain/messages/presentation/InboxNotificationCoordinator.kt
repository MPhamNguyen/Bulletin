package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.application.IncomingMessageNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Observes the single session inbox and forwards new incoming messages to the platform notifier.
 * It reads the shared inbox state; it does not own a second inbox or subscription.
 */
class InboxNotificationCoordinator(
    private val inbox: StateFlow<InboxState>,
    private val activeConversation: ActiveConversationTracker,
    private val notifier: IncomingMessageNotifier,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val detector = IncomingMessageAlertDetector()
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
                detection.alerts.forEach(notifier::show)
            }
        }
    }

    fun close() {
        scope.cancel()
        job = null
    }
}
