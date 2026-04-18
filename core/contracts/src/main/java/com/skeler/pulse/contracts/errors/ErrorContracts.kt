package com.skeler.pulse.contracts.errors

import com.skeler.pulse.contracts.TraceId
import java.time.Instant

sealed interface PulseError {
    val code: String
    val message: String
    val isRetryable: Boolean
}

sealed interface MessagingSurfaceError : PulseError

sealed interface NetworkError : MessagingSurfaceError {
    data class Timeout(
        override val message: String = "Network timeout",
        override val isRetryable: Boolean = true,
    ) : NetworkError {
        override val code: String = "network_timeout"
    }

    data class RateLimited(
        val retryAt: Instant,
        override val message: String = "Rate limited",
        override val isRetryable: Boolean = true,
    ) : NetworkError {
        override val code: String = "network_rate_limited"
    }

    data class Unreachable(
        override val message: String = "Network unreachable",
        override val isRetryable: Boolean = true,
    ) : NetworkError {
        override val code: String = "network_unreachable"
    }
}

sealed interface ProtocolError : MessagingSurfaceError {
    data class SecureChannelUnavailable(
        override val message: String = "Secure channel unavailable",
        override val isRetryable: Boolean = true,
    ) : ProtocolError {
        override val code: String = "protocol_secure_channel_unavailable"
    }

    data class KeyStoreUnavailable(
        override val message: String = "Secure key storage unavailable",
        override val isRetryable: Boolean = false,
    ) : ProtocolError {
        override val code: String = "protocol_keystore_unavailable"
    }

    data class KeyRotationRequired(
        override val message: String = "Key rotation required",
        override val isRetryable: Boolean = true,
    ) : ProtocolError {
        override val code: String = "protocol_key_rotation_required"
    }
}

sealed interface SystemError : MessagingSurfaceError {
    data class PersistenceFailure(
        override val message: String = "Local persistence failure",
        override val isRetryable: Boolean = true,
    ) : SystemError {
        override val code: String = "system_persistence_failure"
    }

    data class ValidationFailure(
        override val message: String = "Message validation failed",
        override val isRetryable: Boolean = false,
    ) : SystemError {
        override val code: String = "system_validation_failure"
    }
}

sealed interface DiagnosticError : PulseError {
    val traceId: TraceId
}
