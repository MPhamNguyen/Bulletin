package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageEdit
import com.jdrms.bulletin.domain.messages.domain.model.MessageEditException
import com.jdrms.bulletin.domain.messages.domain.model.MessageEditFailure
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.domain.service.MessagesPolicy

class EditMessage(
    private val repository: MessagesRepository,
    private val currentSenderProvider: CurrentMessageSenderProvider
) {
    suspend operator fun invoke(edit: MessageEdit): Result<Message> {
        val sender = when (val result = currentSenderProvider.getCurrentSender()) {
            is Result.Success -> result.data
            is Result.Error -> return result
        }
        val normalized = edit.copy(content = edit.content.trim())
        if (MessagesPolicy().validateMessageContent(normalized.content) is Result.Error) {
            return Result.Error(MessageEditException(MessageEditFailure.INVALID))
        }
        return repository.editMessage(sender.id, normalized)
    }
}
