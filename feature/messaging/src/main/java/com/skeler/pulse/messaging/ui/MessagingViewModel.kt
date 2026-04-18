package com.skeler.pulse.messaging.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skeler.pulse.contracts.ConversationId
import com.skeler.pulse.contracts.messaging.BusinessPriority
import com.skeler.pulse.contracts.messaging.MessagingState
import com.skeler.pulse.contracts.messaging.SendEligibility
import com.skeler.pulse.messaging.domain.ObserveConversationUseCase
import com.skeler.pulse.messaging.domain.RequestConversationRefreshUseCase
import com.skeler.pulse.messaging.domain.SendMessageUseCase
import com.skeler.pulse.messaging.model.MessagingIntent
import com.skeler.pulse.messaging.model.MessagingMutation
import com.skeler.pulse.messaging.model.MessagingReducer
import com.skeler.pulse.messaging.model.MessagingStateFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MessagingViewModel(
    initialConversationId: ConversationId,
    private val observeConversation: ObserveConversationUseCase,
    private val requestConversationRefresh: RequestConversationRefreshUseCase,
    private val sendMessage: SendMessageUseCase,
) : ViewModel() {

    private var currentConversationId: ConversationId = initialConversationId
    private var observationJob: Job? = null

    private val mutableState = MutableStateFlow(MessagingStateFactory.initial(initialConversationId))
    val state: StateFlow<MessagingState> = mutableState.asStateFlow()

    init {
        bindConversation(initialConversationId)
    }

    fun accept(intent: MessagingIntent) {
        when (intent) {
            MessagingIntent.LoadConversation -> refreshConversation()
            is MessagingIntent.DraftChanged -> mutate(MessagingMutation.DraftUpdated(intent.value))
            is MessagingIntent.PriorityChanged -> updatePriority(intent.priority)
            MessagingIntent.SendPressed -> sendDraft()
        }
    }

    fun bindConversation(conversationId: ConversationId) {
        if (conversationId == currentConversationId && observationJob != null) {
            return
        }
        currentConversationId = conversationId
        mutableState.value = MessagingStateFactory.initial(conversationId)
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            requestConversationRefresh(conversationId)
            observeConversation(conversationId).collectLatest { snapshot ->
                mutate(MessagingMutation.HistoryLoaded(snapshot))
            }
        }
    }

    private fun refreshConversation() {
        viewModelScope.launch {
            requestConversationRefresh(currentConversationId)
        }
    }

    private fun updatePriority(priority: BusinessPriority) {
        val current = mutableState.value
        val updatedComposer = current.composer.copy(
            draft = current.composer.draft.copy(priority = priority),
        )
        mutableState.value = when (current) {
            is MessagingState.Blocked -> current.copy(composer = updatedComposer)
            is MessagingState.Idle -> current.copy(composer = updatedComposer)
            is MessagingState.LoadingHistory -> current.copy(composer = updatedComposer)
            is MessagingState.Ready -> current.copy(composer = updatedComposer)
            is MessagingState.Recovering -> current.copy(composer = updatedComposer)
            is MessagingState.Sending -> current.copy(composer = updatedComposer)
        }
    }

    private fun sendDraft() {
        val current = mutableState.value
        if (current.composer.draft.text.isBlank() || current.composer.eligibility is SendEligibility.Blocked) {
            return
        }

        val messageId = java.util.UUID.randomUUID().toString()
        mutate(MessagingMutation.SendingStarted(messageId))
        viewModelScope.launch {
            when (val result = sendMessage(currentConversationId, mutableState.value.composer.draft)) {
                is com.skeler.pulse.messaging.api.SendMessageResult.Failure ->
                    mutate(MessagingMutation.SendFailed(result.error))

                is com.skeler.pulse.messaging.api.SendMessageResult.Success ->
                    mutate(MessagingMutation.SendingCompleted)
            }
        }
    }

    private fun mutate(mutation: MessagingMutation) {
        mutableState.value = MessagingReducer.reduce(mutableState.value, mutation)
    }
}
