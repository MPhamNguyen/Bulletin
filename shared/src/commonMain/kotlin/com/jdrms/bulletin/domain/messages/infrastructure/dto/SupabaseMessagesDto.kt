package com.jdrms.bulletin.domain.messages.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseConversationDto(
    @SerialName("id") val id: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("listing_id") val listingId: String? = null
)

@Serializable
data class SupabaseConversationLookupDto(@SerialName("conversation_id") val conversationId: String)

@Serializable
data class SupabaseConversationMembershipDto(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("history_visible_after") val historyVisibleAfter: String? = null
)

@Serializable
data class SupabaseConversationInboxDto(
    @SerialName("id") val id: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("listing_id") val listingId: String? = null,
    @SerialName("membership") val membership: List<SupabaseInboxMembershipDto> = emptyList(),
    @SerialName("participants") val participants: List<SupabaseInboxParticipantDto> = emptyList(),
    @SerialName("messages") val latestMessages: List<SupabaseMessageDto> = emptyList()
)

@Serializable
data class SupabaseInboxMembershipDto(
    @SerialName("user_id") val userId: String,
    @SerialName("history_visible_after") val historyVisibleAfter: String? = null
)

@Serializable
data class SupabaseInboxParticipantDto(
    @SerialName("user_id") val userId: String,
    @SerialName("profiles") val profile: SupabaseMessageProfileDto? = null,
    @SerialName("last_read_at") val lastReadAt: String? = null
)

@Serializable
data class SupabaseConversationParticipantDto(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("profiles") val profile: SupabaseMessageProfileDto? = null,
    @SerialName("last_read_at") val lastReadAt: String? = null
)

@Serializable
data class SupabaseMessageProfileDto(
    @SerialName("full_name") val fullName: String? = null
)

@Serializable
data class SupabaseMessageDto(
    @SerialName("id") val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("body") val content: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("profiles") val senderProfile: SupabaseMessageProfileDto? = null,
    @SerialName("reports") val reports: List<SupabaseMessageReportMarkerDto> = emptyList(),
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class SupabaseMessageReportMarkerDto(
    @SerialName("id") val id: String,
    @SerialName("status") val status: String? = null
)

@Serializable
data class SupabaseMessageInsertDto(
    @SerialName("id") val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("body") val content: String
)

@Serializable
data class SupabaseMessageReportInsertDto(
    @SerialName("message_id") val messageId: String,
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("reason") val reason: String,
    @SerialName("type") val type: SupabaseMessageReportType,
    @SerialName("description") val description: String?
)
