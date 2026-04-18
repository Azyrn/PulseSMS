package com.skeler.pulse.sync.model

import com.skeler.pulse.contracts.ConversationId

data class SyncRequest(
    val conversationId: ConversationId,
)
