package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageCursor
import com.jdrms.bulletin.domain.messages.domain.model.MessageReportAlreadySubmittedException
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationInboxDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationMembershipDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationParticipantDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseInboxMembershipDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseInboxParticipantDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageInsertDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageProfileDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageReportInsertDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageReportMarkerDto
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesTable
import kotlin.time.Instant

internal class FakeSupabaseMessagesTable(
    conversations: List<Conversation>,
    messages: List<Message> = emptyList(),
    var authenticatedUserId: String? = alice.id.value
) : SupabaseMessagesTable {
    private var clockMillis = maxOf(messages.maxOfOrNull(Message::timestampMillis) ?: 0L, 41L)
    private fun serverNow(): Instant = Instant.fromEpochMilliseconds(++clockMillis)
    private val conversationRows = conversations.associate { conversation ->
        conversation.id.value to SupabaseConversationDto(
            id = conversation.id.value,
            updatedAt = Instant.fromEpochMilliseconds(conversation.updatedAtMillis).toString(),
            listingId = conversation.listingId?.value
        )
    }.toMutableMap()
    private val participantRows = conversations.flatMap { conversation ->
        conversation.participants.map { participant ->
            SupabaseConversationParticipantDto(
                conversationId = conversation.id.value,
                userId = participant.id.value,
                profile = SupabaseMessageProfileDto(participant.displayName),
                lastReadAt = participant.lastReadAt?.toString()
            )
        }
    }.toMutableList()
    private val messageRows = messages.map(::toMessageDto).toMutableList()
    private val hiddenBy = mutableMapOf<String, MutableSet<String>>()
    private val historyVisibleAfter = mutableMapOf<Pair<String, String>, Instant>()
    private val activeReports = mutableSetOf<Pair<String, String>>()

    var insertedMessage: SupabaseMessageInsertDto? = null
    var insertedReport: SupabaseMessageReportInsertDto? = null
    var inboxReadCount = 0
        private set

    override suspend fun getAuthenticatedUserId(): String? = authenticatedUserId

    override suspend fun getOrCreateConversation(requesterId: String, sellerId: String, listingId: String): String {
        val members = setOf(requesterId, sellerId)
        val existingId = conversationRows.values.firstOrNull { row ->
            row.listingId == listingId && participantRows
                .filter { it.conversationId == row.id }
                .map { it.userId }
                .toSet() == members
        }?.id
        if (existingId != null) return existingId

        val createdId = "created-conversation-${conversationRows.size + 1}"
        conversationRows[createdId] = SupabaseConversationDto(createdId, listingId = listingId)
        participantRows += SupabaseConversationParticipantDto(
            conversationId = createdId,
            userId = requesterId,
            profile = SupabaseMessageProfileDto("Student")
        )
        participantRows += SupabaseConversationParticipantDto(
            conversationId = createdId,
            userId = sellerId,
            profile = SupabaseMessageProfileDto("Carol")
        )
        return createdId
    }

    override suspend fun getInboxConversations(userId: String): List<SupabaseConversationInboxDto> {
        inboxReadCount++
        return participantRows
            .filter { it.userId == userId && userId !in hiddenBy[it.conversationId].orEmpty() }
            .map { it.conversationId }
            .distinct()
            .mapNotNull { id ->
                val conversation = conversationRows[id] ?: return@mapNotNull null
                SupabaseConversationInboxDto(
                    id = conversation.id,
                    updatedAt = conversation.updatedAt,
                    listingId = conversation.listingId,
                    membership = listOf(
                        SupabaseInboxMembershipDto(userId, historyVisibleAfter[id to userId]?.toString())
                    ),
                    participants = participantRows.filter { it.conversationId == id }.map { participant ->
                        SupabaseInboxParticipantDto(
                            participant.userId,
                            participant.profile,
                            participant.lastReadAt
                        )
                    },
                    latestMessages = listOfNotNull(
                        getMessagePage(id, null, historyVisibleAfter[id to userId], 1).firstOrNull()
                    )
                )
            }
    }

    override suspend fun hideConversation(conversationId: String): Boolean {
        val viewerId = authenticatedUserId ?: return false
        if (!isParticipant(conversationId, viewerId)) return false
        if (hiddenBy.getOrPut(conversationId) { mutableSetOf() }.add(viewerId)) {
            historyVisibleAfter[conversationId to viewerId] = serverNow()
        }
        return true
    }

    override suspend fun restoreConversation(conversationId: String): Boolean {
        val viewerId = authenticatedUserId ?: return false
        if (!isParticipant(conversationId, viewerId)) return false
        hiddenBy[conversationId]?.remove(viewerId)
        return true
    }

    override suspend fun findConversation(conversationId: String): SupabaseConversationDto? =
        conversationRows[conversationId]

    override suspend fun isParticipant(conversationId: String, userId: String): Boolean =
        participantRows.any { it.conversationId == conversationId && it.userId == userId }

    override suspend fun getMembership(conversationId: String, userId: String): SupabaseConversationMembershipDto? =
        if (isParticipant(conversationId, userId)) {
            SupabaseConversationMembershipDto(conversationId, historyVisibleAfter[conversationId to userId]?.toString())
        } else {
            null
        }

    override suspend fun markConversationRead(conversationId: String): Instant? {
        val viewerId = authenticatedUserId ?: return null
        val index = participantRows.indexOfFirst {
            it.conversationId == conversationId && it.userId == viewerId
        }
        if (index < 0) return null
        val markedAt = serverNow()
        participantRows[index] = participantRows[index].copy(lastReadAt = markedAt.toString())
        return markedAt
    }

    override suspend fun getParticipants(conversationId: String): List<SupabaseConversationParticipantDto> =
        participantRows.filter { it.conversationId == conversationId }

    override suspend fun getMessagePage(
        conversationId: String,
        before: MessageCursor?,
        visibleAfter: Instant?,
        limit: Int
    ): List<SupabaseMessageDto> = messageRows
        .filter { it.conversationId == conversationId }
        .filter { visibleAfter == null || Instant.parse(it.createdAt) > visibleAfter }
        .filter {
            before == null || Instant.parse(it.createdAt) < before.createdAt ||
                (Instant.parse(it.createdAt) == before.createdAt && it.id < before.id.value)
        }
        .sortedWith(compareByDescending<SupabaseMessageDto> { it.createdAt }.thenByDescending { it.id })
        .take(limit)

    override suspend fun findMessage(conversationId: String, messageId: String): SupabaseMessageDto? =
        messageRows.find { it.conversationId == conversationId && it.id == messageId }

    override suspend fun findLatestMessage(conversationId: String): SupabaseMessageDto? =
        getMessagePage(conversationId, null, null, 1).firstOrNull()

    override suspend fun insertMessage(message: SupabaseMessageInsertDto) {
        insertedMessage = message
        val senderId = checkNotNull(authenticatedUserId)
        val participant = participantRows.first { row ->
            row.conversationId == message.conversationId && row.userId == senderId
        }
        messageRows += SupabaseMessageDto(
            id = message.id,
            conversationId = message.conversationId,
            senderId = senderId,
            content = message.content,
            createdAt = serverNow().toString(),
            senderProfile = participant.profile
        )
        val conversation = conversationRows.getValue(message.conversationId)
        conversationRows[message.conversationId] = conversation.copy(updatedAt = messageRows.last().createdAt)
        hiddenBy.remove(message.conversationId)
    }

    override suspend fun insertReport(report: SupabaseMessageReportInsertDto) {
        val index = messageRows.indexOfFirst { it.id == report.messageId }
        check(index >= 0)
        if (!activeReports.add(report.reporterId to report.messageId)) throw MessageReportAlreadySubmittedException()
        insertedReport = report
        messageRows[index] = messageRows[index].copy(
            reports = messageRows[index].reports + SupabaseMessageReportMarkerDto("report-${report.messageId}")
        )
    }

    fun replaceMessageTimestamp(timestamp: String) {
        check(messageRows.isNotEmpty())
        messageRows[0] = messageRows[0].copy(createdAt = timestamp)
    }

    fun closeReport(reporterId: String, messageId: String): Boolean =
        activeReports.remove(reporterId to messageId)

    private fun toMessageDto(message: Message): SupabaseMessageDto = SupabaseMessageDto(
        id = message.id.value,
        conversationId = message.conversationId.value,
        senderId = message.senderId.value,
        content = message.content,
        createdAt = Instant.fromEpochMilliseconds(message.timestampMillis).toString(),
        senderProfile = SupabaseMessageProfileDto(message.senderName),
        reports = if (message.isReported) {
            listOf(SupabaseMessageReportMarkerDto("existing-report"))
        } else {
            emptyList()
        }
    )
}
