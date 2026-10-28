package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.service.MessagesPolicy
import com.jdrms.bulletin.domain.reporting.domain.model.ReportReason
import com.jdrms.bulletin.domain.reporting.domain.model.ReportType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class MessageReportActions(
    private val state: MutableStateFlow<MessagesUiState>,
    private val inbox: InboxStateOwner,
    private val actions: MessagesActions,
    private val selectConversation: (ConversationId) -> Unit,
    private val showFailure: (Result.Error) -> Unit
) {
    fun begin(messageId: MessageId) {
        val message = state.value.currentMessages.firstOrNull { it.id == messageId } ?: return
        if (message.isReported || message.senderId == state.value.viewerId) return
        state.update { it.copy(messageReportForm = MessageReportFormState(messageId), errorMessage = null) }
    }

    fun selectReason(reason: ReportReason) {
        if (!reason.appliesTo(ReportType.MESSAGE)) return
        state.update { current ->
            current.copy(
                messageReportForm = current.messageReportForm?.copy(selectedReason = reason, errorMessage = null),
                errorMessage = null
            )
        }
    }

    fun updateDescription(description: String) {
        if (description.length > MessagesPolicy.MAX_REPORT_DESCRIPTION_LENGTH) return
        state.update { current ->
            current.copy(
                messageReportForm = current.messageReportForm?.copy(description = description, errorMessage = null)
            )
        }
    }

    fun dismiss() {
        if (!state.value.isReporting) {
            state.update { it.copy(messageReportForm = null, errorMessage = null) }
        }
    }

    fun submit(applyInboxSelection: (InboxState) -> Unit) {
        val form = state.value.messageReportForm ?: return
        if (state.value.isReporting || form.isSubmitted) return
        val reason = form.selectedReason ?: run {
            state.update { it.copy(messageReportForm = form.copy(errorMessage = "Choose a reason for your report.")) }
            return
        }
        inbox.synchronizeIdentity()
        applyInboxSelection(inbox.state.value)
        if (inbox.state.value.isLoading) {
            state.update { current ->
                current.copy(
                    messageReportForm = current.messageReportForm?.copy(
                        errorMessage = "Please wait for conversations to finish loading, then try again."
                    )
                )
            }
            return
        }
        actions.report(
            form.messageId,
            reason,
            form.description.trim(),
            onReported = { conversationId ->
                state.update { current ->
                    current.copy(messageReportForm = current.messageReportForm?.copy(isSubmitted = true))
                }
                selectConversation(conversationId)
            },
            showFailure = showFailure
        )
    }
}
