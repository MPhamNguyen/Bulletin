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
    @SerialName("conversation_id") val conversationId: String
)

@Serializable
data class SupabaseConversationParticipantDto(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("profiles") val profile: SupabaseMessageProfileDto? = null
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
    @SerialName("message_reports") val reports: List<SupabaseMessageReportMarkerDto> = emptyList()
)

@Serializable
data class SupabaseMessageReportMarkerDto(
    @SerialName("id") val id: String
)

@Serializable
data class SupabaseMessageInsertDto(
    @SerialName("id") val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("body") val content: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class SupabaseMessageReportInsertDto(
    @SerialName("message_id") val messageId: String,
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("reason") val reason: String
)
