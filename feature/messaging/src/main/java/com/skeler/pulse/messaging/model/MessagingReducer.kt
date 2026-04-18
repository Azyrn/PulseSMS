package com.skeler.pulse.messaging.model

import com.skeler.pulse.contracts.messaging.ComposerState
import com.skeler.pulse.contracts.messaging.ComposerTransition
import com.skeler.pulse.contracts.messaging.ConversationSyncState
import com.skeler.pulse.contracts.messaging.MessageDraft
import com.skeler.pulse.contracts.messaging.MessagingState

object MessagingReducer {
    fun reduce(
        previous: MessagingState,
        mutation: MessagingMutation,
    ): MessagingState = when (mutation) {
        is MessagingMutation.HistoryLoaded ->
            MessagingState.Ready(
                conversationId = previous.conversationId,
                timeline = mutation.snapshot.timeline,
                composer = previous.composer.copy(
                    eligibility = mutation.snapshot.eligibility,
                    transition = null,
                ),
                sync = mutation.snapshot.syncState,
                lastSyncedAt = mutation.snapshot.lastSyncedAt,
                complianceUpdatedAt = mutation.snapshot.complianceUpdatedAt,
                surfaceError = null,
            )

        is MessagingMutation.DraftUpdated ->
            previous.withComposer(
                previous.composer.copy(
                    draft = previous.composer.draft.copy(text = mutation.value),
                    transition = null,
                )
            )

        is MessagingMutation.SendingStarted ->
            MessagingState.Sending(
                conversationId = previous.conversationId,
                timeline = previous.timeline,
                composer = previous.composer.copy(
                    transition = ComposerTransition.SendStarted,
                ),
                sync = previous.sync,
                lastSyncedAt = previous.lastSyncedAt,
                complianceUpdatedAt = previous.complianceUpdatedAt,
                surfaceError = null,
                inFlightMessageId = mutation.messageId,
            )

        is MessagingMutation.SendingCompleted ->
            MessagingState.Ready(
                conversationId = previous.conversationId,
                timeline = previous.timeline,
                composer = previous.composer.copy(
                    draft = MessageDraft.empty(),
                    transition = ComposerTransition.SendSucceeded,
                ),
                sync = ConversationSyncState.Syncing,
                lastSyncedAt = previous.lastSyncedAt,
                complianceUpdatedAt = previous.complianceUpdatedAt,
                surfaceError = null,
            )

        is MessagingMutation.SendFailed ->
            previous.withComposer(
                composer = previous.composer.copy(
                    transition = ComposerTransition.SendFailed,
                ),
                surfaceError = mutation.error,
            )
    }

    private fun MessagingState.withComposer(
        composer: ComposerState,
        surfaceError: com.skeler.pulse.contracts.errors.MessagingSurfaceError? = this.surfaceError,
    ): MessagingState = when (this) {
        is MessagingState.Blocked -> copy(composer = composer, surfaceError = surfaceError)
        is MessagingState.Idle -> copy(composer = composer, surfaceError = surfaceError)
        is MessagingState.LoadingHistory -> copy(composer = composer, surfaceError = surfaceError)
        is MessagingState.Ready -> copy(composer = composer, surfaceError = surfaceError)
        is MessagingState.Recovering -> copy(composer = composer, surfaceError = surfaceError)
        is MessagingState.Sending -> copy(composer = composer, surfaceError = surfaceError)
    }
}
