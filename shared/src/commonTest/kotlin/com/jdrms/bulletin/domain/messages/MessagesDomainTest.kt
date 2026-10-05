package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.service.MessagesPolicy
import com.jdrms.bulletin.domain.messages.infrastructure.dto.ConversationDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.ConversationParticipantDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.MessageDto
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.MessagesMapper
import com.jdrms.bulletin.domain.messages.infrastructure.repository.InMemoryMessagesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MessagesDomainTest {

    private val policy = MessagesPolicy()

    @Test
    fun testValidMessagePasses() {
        val result = policy.validateMessageContent("Hello, is the item still available?")
        assertTrue(result.isSuccess())
    }

    @Test
    fun testBlankMessageFails() {
        val result = policy.validateMessageContent("    ")
        assertTrue(result.isError())
    }

    @Test
    fun testBlankReportReasonFails() {
        val result = policy.validateReportReason("")
        assertTrue(result.isError())
    }

    @Test
    fun testBlankContentInMessageModelThrows() {
        assertFailsWith<IllegalArgumentException> {
            Message(
                id = MessageId("m1"),
                conversationId = ConversationId("c1"),
                senderId = SenderId("s1"),
                senderName = "Dominic",
                content = "   "
            )
        }
    }

    @Test
    fun testSendMessageAndReport() = runTest {
        val senderId = SenderId("student_1")
        val repo = InMemoryMessagesRepository(
            initialConversations = listOf(
                ConversationDto("conv_1", listOf(ConversationParticipantDto(senderId.value, "Dominic")))
            )
        )
        val convId = ConversationId("conv_1")
        val msg = Message(
            id = MessageId("msg_new_1"),
            conversationId = convId,
            senderId = SenderId("student_1"),
            senderName = "Dominic",
            content = "Can we meet at the bookstore?"
        )

        val sendResult = repo.sendMessage(msg)
        assertTrue(sendResult.isSuccess())

        val messages = repo.getMessages(senderId, convId).getOrThrow()
        assertTrue(messages.any { it.id == MessageId("msg_new_1") })

        val reportResult = repo.reportMessage(senderId, convId, MessageId("msg_new_1"), "Spam")
        assertTrue(reportResult.isSuccess())

        val updatedMessages = repo.getMessages(senderId, convId).getOrThrow()
        val reportedMsg = updatedMessages.first { it.id == MessageId("msg_new_1") }
        assertTrue(reportedMsg.isReported)
    }

    @Test
    fun testMapperRoundTrip() {
        val dto = MessageDto(
            id = "msg_1",
            conversationId = "conv_1",
            senderId = "s_1",
            senderName = "Sean",
            content = "Ready when you are",
            timestampMillis = 12345L,
            isReported = false
        )
        val domain = MessagesMapper.toDomain(dto)
        assertEquals("Ready when you are", domain.content)

        val backToDto = MessagesMapper.toDto(domain)
        assertEquals(dto.id, backToDto.id)
        assertEquals(dto.content, backToDto.content)
        assertEquals(dto, backToDto)
    }

    @Test
    fun conversationMapperPreservesParticipantIdsIndependentlyOfNames() {
        val dto = ConversationDto(
            id = "conversation",
            participants = listOf(
                ConversationParticipantDto("user-a", "Same Name"),
                ConversationParticipantDto("user-b", "Same Name")
            ),
            lastMessage = MessageDto("message", "conversation", "user-b", "Same Name", "Hello", 42L),
            updatedAtMillis = 42L
        )
        val domain = MessagesMapper.toDomain(dto)
        assertEquals(listOf(SenderId("user-a"), SenderId("user-b")), domain.participants.map { it.id })
        assertEquals(dto, MessagesMapper.toDto(domain))
        assertEquals(
            dto.copy(lastMessage = null),
            MessagesMapper.toDto(MessagesMapper.toDomain(dto.copy(lastMessage = null)))
        )
    }

    @Test
    fun mapperRejectsInvalidParticipantIdentity() {
        assertFailsWith<IllegalArgumentException> {
            MessagesMapper.toDomain(ConversationDto("conversation", listOf(ConversationParticipantDto("", "Name"))))
        }
    }
}
