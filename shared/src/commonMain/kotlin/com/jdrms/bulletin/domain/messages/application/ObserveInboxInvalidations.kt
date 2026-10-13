package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.InboxInvalidation
import com.jdrms.bulletin.domain.messages.domain.repository.InboxObserver
import kotlinx.coroutines.flow.Flow

class ObserveInboxInvalidations(private val observer: InboxObserver) {
    operator fun invoke(userId: SenderId): Flow<InboxInvalidation> = observer.observe(userId)
}
