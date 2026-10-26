package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.app.integration.AuthInboxIdentity
import com.jdrms.bulletin.app.integration.AuthMessageSenderProvider
import com.jdrms.bulletin.app.integration.MarketplaceConversationListingPreviewProvider
import com.jdrms.bulletin.domain.messages.application.ConsoleMessagesInstrumentation
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.HasListingConversation
import com.jdrms.bulletin.domain.messages.application.HideConversation
import com.jdrms.bulletin.domain.messages.application.MarkConversationRead
import com.jdrms.bulletin.domain.messages.application.MessageSeller
import com.jdrms.bulletin.domain.messages.application.ObserveConversationMessages
import com.jdrms.bulletin.domain.messages.application.ObserveInboxInvalidations
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.repository.InboxObserver
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesObserver
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.repository.InMemoryMessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseInboxObserver
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesObserver
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesRepository
import com.jdrms.bulletin.domain.messages.presentation.InboxStateOwner

internal class MessagesModule(
    private val infrastructure: InfrastructureModule,
    private val profile: ProfileModule,
    private val listings: ListingsModule,
    private val marketplace: MarketplaceModule
) {
    private val inMemoryMessages by lazy { InMemoryMessagesRepository() }
    internal val messagesRepository: MessagesRepository by lazy {
        infrastructure.backend(
            supabase = { SupabaseMessagesRepository(it, instrumentation = ConsoleMessagesInstrumentation) },
            inMemory = { inMemoryMessages }
        )
    }
    internal val messagesObserver: MessagesObserver by lazy {
        infrastructure.backend(
            supabase = { SupabaseMessagesObserver(it, messagesRepository) },
            inMemory = { inMemoryMessages }
        )
    }
    internal val inboxObserver: InboxObserver by lazy {
        infrastructure.backend(
            supabase = ::SupabaseInboxObserver,
            inMemory = { inMemoryMessages }
        )
    }
    val currentMessageSenderProvider by lazy { AuthMessageSenderProvider(profile.restoreAuthenticatedProfile) }
    val getConversations by lazy { GetConversations(messagesRepository, currentMessageSenderProvider) }
    val conversationListingPreviewProvider by lazy {
        MarketplaceConversationListingPreviewProvider(listings.listingsRepository, marketplace.marketplaceRepository)
    }
    internal val recentListingBuyersProvider by lazy { createRecentListingBuyersProvider(getConversations) }
    val hideConversation by lazy { HideConversation(messagesRepository, currentMessageSenderProvider) }
    val hasListingConversation by lazy { HasListingConversation(messagesRepository, currentMessageSenderProvider) }
    val getConversationMessages by lazy { GetConversationMessages(messagesRepository, currentMessageSenderProvider) }
    val markConversationRead by lazy { MarkConversationRead(messagesRepository, currentMessageSenderProvider) }
    val observeConversationMessages by lazy {
        val observer = messagesObserver
        val sender = currentMessageSenderProvider
        ObserveConversationMessages(observer, sender)
    }
    val observeInboxInvalidations by lazy { ObserveInboxInvalidations(inboxObserver) }
    val inboxStateOwner by lazy {
        InboxStateOwner(getConversations, observeInboxInvalidations, AuthInboxIdentity(profile.sessionRepository))
    }
    val sendMessage by lazy { SendMessage(messagesRepository, currentMessageSenderProvider) }
    val messageSeller by lazy { MessageSeller(messagesRepository, currentMessageSenderProvider) }
    val reportMessage by lazy { ReportMessage(messagesRepository, currentMessageSenderProvider) }
}
