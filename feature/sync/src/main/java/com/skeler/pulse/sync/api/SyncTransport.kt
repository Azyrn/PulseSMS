package com.skeler.pulse.sync.api

import com.skeler.pulse.contracts.persistence.PersistedMessageEnvelope

interface SyncTransport {
    suspend fun send(
        message: PersistedMessageEnvelope,
    ): SyncTransportResult
}

sealed interface SyncTransportResult {
    data object Success : SyncTransportResult

    data class RetryableFailure(
        val code: String,
    ) : SyncTransportResult

    data class PermanentFailure(
        val code: String,
    ) : SyncTransportResult
}
