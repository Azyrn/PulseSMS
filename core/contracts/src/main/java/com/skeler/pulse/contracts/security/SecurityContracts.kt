package com.skeler.pulse.contracts.security

sealed interface KeyStoreCapability {
    data class Available(
        val hardwareBacked: Boolean,
    ) : KeyStoreCapability

    data object SoftwareOnly : KeyStoreCapability
    data object Unavailable : KeyStoreCapability
}

sealed interface KeyManagementState {
    data object Ready : KeyManagementState
    data object RotationRequired : KeyManagementState
    data object Corrupted : KeyManagementState
    data object Unrecoverable : KeyManagementState
}
