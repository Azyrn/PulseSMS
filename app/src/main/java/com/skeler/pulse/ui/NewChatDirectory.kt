package com.skeler.pulse.ui

import android.content.Context
import android.provider.ContactsContract
import android.telephony.SubscriptionManager
import androidx.compose.runtime.Immutable
import com.skeler.pulse.contact.normalizeAddressForDisplay
import com.skeler.pulse.sms.SmsThread

@Immutable
internal data class NewChatRecipient(
    val key: String,
    val displayName: String,
    val address: String,
    val sortLabel: String,
)

@Immutable
internal data class NewChatSimOption(
    val key: String,
    val subscriptionId: Int?,
    val slotLabel: String,
    val carrierLabel: String,
)

internal fun loadNewChatRecipients(context: Context): List<NewChatRecipient> {
    val recipients = linkedMapOf<String, NewChatRecipient>()

    try {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC",
        )

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
            val numberIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val rawNumber = it.getString(numberIndex).orEmpty().trim()
                val normalizedAddress = rawNumber.normalizeAddressForDisplay()
                if (normalizedAddress.isBlank()) continue

                val displayName = it.getString(nameIndex).orEmpty().trim().ifBlank { rawNumber }
                val contactId = it.getLong(idIndex)
                recipients.putIfAbsent(
                    normalizedAddress,
                    NewChatRecipient(
                        key = "contact_${contactId}_$normalizedAddress",
                        displayName = displayName,
                        address = rawNumber,
                        sortLabel = displayName,
                    ),
                )
            }
        }
    } catch (_: SecurityException) {
        // Contact access not granted. The UI falls back to recent conversation addresses.
    }

    return recipients.values.sortedWith(
        compareBy<NewChatRecipient> { it.sortLabel.lowercase() }
            .thenBy { it.address.lowercase() },
    )
}

internal fun List<NewChatRecipient>.mergeThreadRecipients(threads: List<SmsThread>): List<NewChatRecipient> {
    val recipients = linkedMapOf<String, NewChatRecipient>()

    forEach { recipient ->
        val normalizedAddress = recipient.address.normalizeAddressForDisplay()
        if (normalizedAddress.isNotBlank()) {
            recipients.putIfAbsent(normalizedAddress, recipient)
        }
    }

    threads.forEach { thread ->
        val normalizedAddress = thread.address.normalizeAddressForDisplay()
        if (normalizedAddress.isBlank()) return@forEach
        recipients.putIfAbsent(
            normalizedAddress,
            NewChatRecipient(
                key = "thread_${thread.threadId}_$normalizedAddress",
                displayName = thread.address,
                address = thread.address,
                sortLabel = thread.address,
            ),
        )
    }

    return recipients.values.sortedWith(
        compareBy<NewChatRecipient> { it.sortLabel.lowercase() }
            .thenBy { it.address.lowercase() },
    )
}

internal fun loadSimOptions(context: Context): List<NewChatSimOption> {
    val fallback = listOf(
        NewChatSimOption(
            key = "sim_default",
            subscriptionId = null,
            slotLabel = "SIM 1",
            carrierLabel = "Default line",
        ),
    )

    return try {
        val manager = context.getSystemService(SubscriptionManager::class.java) ?: return fallback
        val activeSubscriptions = manager.activeSubscriptionInfoList.orEmpty()
            .sortedBy { it.simSlotIndex }
            .map { info ->
                val carrierLabel = info.carrierName?.toString()?.trim().orEmpty()
                    .ifBlank {
                        info.displayName?.toString()?.trim().orEmpty().ifBlank { "Primary line" }
                    }
                NewChatSimOption(
                    key = "sim_${info.subscriptionId}",
                    subscriptionId = info.subscriptionId,
                    slotLabel = "SIM ${info.simSlotIndex + 1}",
                    carrierLabel = carrierLabel,
                )
            }
            .distinctBy { it.key }

        if (activeSubscriptions.isEmpty()) fallback else activeSubscriptions
    } catch (_: SecurityException) {
        fallback
    }
}

