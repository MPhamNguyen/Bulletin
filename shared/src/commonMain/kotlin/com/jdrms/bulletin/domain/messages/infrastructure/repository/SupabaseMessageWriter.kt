package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageEdit
import com.jdrms.bulletin.domain.messages.domain.model.MessageEditException
import com.jdrms.bulletin.domain.messages.domain.model.MessageEditFailure
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.SupabaseMessagesMapper
import io.github.jan.supabase.postgrest.exception.PostgrestRestException

/** Checks current membership before writes; the database must enforce the same rule atomically. */
internal class SupabaseMessageWriter(
    private val table: SupabaseMessagesTable,
    private val requireAuthenticatedUser: suspend (SenderId) -> SenderId
) {
    suspend fun send(message: Message): Message {
        val sender = message.senderId ?: throw ConversationAccessException()
        requireWritable(requireAuthenticatedUser(sender), message.conversationId)
        table.insertMessage(SupabaseMessagesMapper.toInsertDto(message))
        val stored = checkNotNull(table.findMessage(message.conversationId.value, message.id.value)) {
            "Inserted message was not returned"
        }
        return SupabaseMessagesMapper.toMessage(stored)
    }

    suspend fun edit(userId: SenderId, edit: MessageEdit): Message {
        requireWritable(requireAuthenticatedUser(userId), edit.conversationId)
        try {
            return SupabaseMessagesMapper.toMessage(table.editMessage(edit))
        } catch (exception: PostgrestRestException) {
            val failure = when (exception.code) {
                "BM001", "42501" -> MessageEditFailure.UNAVAILABLE
                "BM002" -> MessageEditFailure.EXPIRED
                "BM003" -> MessageEditFailure.REPORTED
                "BM004" -> MessageEditFailure.CONFLICT
                "BM005" -> MessageEditFailure.INVALID
                else -> null
            }
            if (failure != null) throw MessageEditException(failure)
            throw exception
        }
    }

    private suspend fun requireWritable(userId: SenderId, conversationId: ConversationId) {
        val conversation = table.findConversation(conversationId.value) ?: throw ConversationAccessException()
        val participants = table.getParticipants(conversationId.value)
        SupabaseMessagesMapper.toConversation(conversation, participants, null).authorizeSending(userId).getOrThrow()
    }
}
