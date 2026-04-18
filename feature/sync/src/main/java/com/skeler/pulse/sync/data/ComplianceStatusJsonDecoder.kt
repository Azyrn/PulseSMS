package com.skeler.pulse.sync.data

import com.skeler.pulse.security.api.BusinessComplianceStatus
import java.time.Instant

internal object ComplianceStatusJsonDecoder {
    fun decode(json: String): BusinessComplianceStatus =
        BusinessComplianceStatus(
            senderVerified = json.readBoolean("senderVerified"),
            recipientVerified = json.readBoolean("recipientVerified"),
            identityVerified = json.readBoolean("identityVerified"),
            tenDlcRegistered = json.readBoolean("tenDlcRegistered"),
            updatedAt = json.readOptionalInstant("updatedAt"),
        )

    private fun String.readBoolean(key: String): Boolean {
        val pattern = Regex(""""$key"\s*:\s*(true|false)""")
        val match = pattern.find(this)
            ?: throw IllegalArgumentException("Missing boolean field: $key")
        return match.groupValues[1].toBooleanStrict()
    }

    private fun String.readOptionalInstant(key: String): Instant? {
        val pattern = Regex(""""$key"\s*:\s*"([^"]+)"""")
        val value = pattern.find(this)?.groupValues?.get(1) ?: return null
        return Instant.parse(value)
    }
}
