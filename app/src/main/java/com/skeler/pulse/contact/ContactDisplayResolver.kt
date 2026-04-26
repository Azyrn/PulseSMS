package com.skeler.pulse.contact

import android.content.Context
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private val displayNameCache = ConcurrentHashMap<String, String>()

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
    val trimmed = trim()
    if (trimmed.isBlank()) return ""
    return if (trimmed.isLikelyBusinessSender()) {
        trimmed.lowercase(Locale.getDefault())
    } else {
        buildString(trimmed.length) {
            trimmed.forEachIndexed { index, character ->
                when {
                    character.isDigit() -> append(character)
                    character == '+' && isEmpty() && index == 0 -> append(character)
                }
            }
        }
    }
}

private fun lookupContactDisplayName(
    context: Context,
    normalizedAddress: String,
): String? {
    return try {
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
            val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (cursor.moveToNext()) {
                val candidateAddress = cursor.getString(numberIndex).orEmpty().normalizeAddressForDisplay()
                if (candidateAddress != normalizedAddress) continue

                return cursor.getString(nameIndex)
                    .orEmpty()
                    .trim()
                    .ifBlank { null }
            }
            null
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
