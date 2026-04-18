package com.skeler.pulse.sync.api

import com.skeler.pulse.contracts.ConversationId

interface SyncScheduler {
    fun enqueueConversationSync(conversationId: ConversationId)
}
