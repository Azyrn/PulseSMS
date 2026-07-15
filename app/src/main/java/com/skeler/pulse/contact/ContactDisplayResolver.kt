package com.skeler.pulse.contact

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import java.util.Locale
import java.text.Normalizer
import java.util.Collections

private val displayNameCache: MutableMap<String, String> = Collections.synchronizedMap(
    object : LinkedHashMap<String, String>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, String>): Boolean = size > 500
    }
)

internal fun displayNameFor(context: Context, address: String): String {
    val trimmedAddress = address.trim()
    val normalizedAddress = trimmedAddress.normalizeAddressForDisplay()
    if (normalizedAddress.isBlank()) return trimmedAddress.ifBlank { "Unknown sender" }

    displayNameCache[normalizedAddress]?.let { return it }

    val displayName = lookupContactDisplayName(context, normalizedAddress)
        ?: if (trimmedAddress.isLikelyBusinessSender()) {
            trimmedAddress.uppercase(Locale.getDefault())
        } else {
            formatPhoneNumber(trimmedAddress)
        }

    displayNameCache[normalizedAddress] = displayName
    return displayName
}

internal fun String.normalizeAddressForDisplay(): String {
    val trimmed = sanitizeSenderAddress()
    if (trimmed.isBlank()) return ""
    return if (trimmed.isLikelyBusinessSender()) {
        trimmed.lowercase(Locale.ROOT)
    } else {
        buildString(trimmed.length) {
            trimmed.forEach { character ->
                val digit = Character.digit(character, 10)
                when {
                    digit >= 0 -> append(digit)
                    character == '+' && isEmpty() -> append(character)
                }
            }
        }
    }
}

internal fun String.toBlockedSenderKeyOrNull(): String? {
    val sanitized = sanitizeSenderAddress()
    if (sanitized.isBlank()) return null

    sanitized.removeBlockedKeyPrefix("phone:")?.let { phone ->
        return phone.toCanonicalPhoneAddressOrNull()?.let { "phone:$it" }
    }
    sanitized.removeBlockedKeyPrefix("sender:")?.let { sender ->
        return sender.toCanonicalBusinessSenderKey()
    }

    val phoneAddress = sanitized.toCanonicalPhoneAddressOrNull()
    return if (phoneAddress != null) {
        "phone:$phoneAddress"
    } else {
        sanitized.toCanonicalBusinessSenderKey()
    }
}

internal fun Collection<String>.toCanonicalBlockedSenderKeys(): Set<String> =
    fold(emptySet()) { keys, address ->
        val key = address.toBlockedSenderKeyOrNull() ?: return@fold keys
        keys.filterNot { existing -> existing.matchesBlockedSenderKey(key) }.toSet() + key
    }

internal fun String.matchesBlockedSenderKey(other: String): Boolean {
    val left = toBlockedSenderKeyOrNull() ?: return false
    val right = other.toBlockedSenderKeyOrNull() ?: return false
    if (left == right) return true

    val leftPhone = left.removeBlockedKeyPrefix("phone:") ?: return false
    val rightPhone = right.removeBlockedKeyPrefix("phone:") ?: return false
    return leftPhone.isEquivalentUsPhoneAddress(rightPhone)
}

internal fun String.toBlockedSenderDisplayLabel(): String {
    val key = toBlockedSenderKeyOrNull() ?: return "Unknown sender"
    key.removeBlockedKeyPrefix("phone:")?.let { phone ->
        return formatPhoneNumber(phone)
    }
    return key.removeBlockedKeyPrefix("sender:")
        ?.uppercase(Locale.getDefault())
        ?.ifBlank { null }
        ?: "Unknown sender"
}

/**
 * Uses [ContactsContract.PhoneLookup.CONTENT_FILTER_URI] for O(1) indexed lookup
 * instead of scanning the entire Phone table.
 */
private fun lookupContactDisplayName(
    context: Context,
    normalizedAddress: String,
): String? {
    if (normalizedAddress.isBlank()) return null
    val lookupUri = Uri.withAppendedPath(
        ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
        Uri.encode(normalizedAddress),
    )
    return try {
        context.contentResolver.query(
            lookupUri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)?.trim()?.ifBlank { null }
            } else {
                null
            }
        }
    } catch (_: SecurityException) {
        null
    }
}

private fun String.isLikelyBusinessSender(): Boolean = any(Char::isLetter)

private fun formatPhoneNumber(address: String): String {
    val trimmed = address.trim()
    return PhoneNumberUtils.formatNumber(trimmed, Locale.getDefault().country)
        ?.takeIf(String::isNotBlank)
        ?: trimmed
}

private fun String.sanitizeSenderAddress(): String {
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFKC)
    return buildString(normalized.length) {
        normalized.forEach { character ->
            when {
                character.isUnsafeSenderAddressCharacter() -> Unit
                character.isWhitespace() -> append(' ')
                else -> append(character)
            }
        }
    }.trim().replace(Regex("\\s+"), " ")
}

private fun Char.isUnsafeSenderAddressCharacter(): Boolean = when (Character.getType(this)) {
    Character.CONTROL.toInt(),
    Character.FORMAT.toInt(),
    Character.PRIVATE_USE.toInt(),
    Character.SURROGATE.toInt(),
    Character.UNASSIGNED.toInt() -> true
    else -> false
}

private fun String.removeBlockedKeyPrefix(prefix: String): String? =
    if (startsWith(prefix, ignoreCase = true)) {
        drop(prefix.length)
    } else {
        null
    }

private fun String.toCanonicalPhoneAddressOrNull(): String? {
    if (isLikelyBusinessSender()) return null

    var hasLeadingPlus = false
    val digits = buildString(length) {
        this@toCanonicalPhoneAddressOrNull.forEach { character ->
            val digit = Character.digit(character, 10)
            when {
                digit >= 0 -> append(digit)
                character == '+' && isEmpty() && !hasLeadingPlus -> hasLeadingPlus = true
            }
        }
    }
    if (digits.isBlank()) return null

    return when {
        hasLeadingPlus -> "+$digits"
        digits.startsWith("00") && digits.length > 2 -> "+${digits.drop(2)}"
        else -> digits
    }
}

private fun String.toCanonicalBusinessSenderKey(): String? =
    sanitizeSenderAddress()
        .lowercase(Locale.ROOT)
        .replace(" ", "")
        .takeIf(String::isNotBlank)
        ?.let { "sender:$it" }

private fun String.isEquivalentUsPhoneAddress(other: String): Boolean {
    val leftDigits = trimStart('+')
    val rightDigits = other.trimStart('+')
    return when {
        this == other -> true
        this == "+1$rightDigits" && rightDigits.length == 10 -> true
        other == "+1$leftDigits" && leftDigits.length == 10 -> true
        else -> false
    }
}
