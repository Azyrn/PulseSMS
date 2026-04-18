package com.skeler.pulse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.messaging.api.ConversationSummary
import com.skeler.pulse.messaging.domain.ObserveInboxUseCase
import com.skeler.pulse.messaging.domain.RequestConversationRefreshUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class PulseHomeState(
    val conversations: List<ConversationSummary> = emptyList(),
)

class PulseHomeViewModel(
    private val observeInbox: ObserveInboxUseCase,
    private val requestConversationRefresh: RequestConversationRefreshUseCase,
) : ViewModel() {

    private val mutableState = MutableStateFlow(PulseHomeState())
    val state: StateFlow<PulseHomeState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            observeInbox().collectLatest { summaries ->
                mutableState.value = PulseHomeState(conversations = summaries)
            }
        }
    }

    fun refreshConversation(conversationId: ConversationId) {
        viewModelScope.launch {
            requestConversationRefresh(conversationId)
        }
    }

    fun refreshAll(conversationIds: Collection<ConversationId>) {
        viewModelScope.launch {
            conversationIds.distinct().forEach { conversationId ->
                requestConversationRefresh(conversationId)
            }
        }
    }
}
