package com.skeler.pulse.sms

import android.content.Context
import android.net.Uri
import android.provider.Telephony

internal object MmsAddressResolver {

    /**
     * Resolves the other-party address for an MMS message.
     *
     * @param msgBox the `msg_box` value from the MMS provider (e.g.
     *   [Telephony.Mms.MESSAGE_BOX_INBOX] or [Telephony.Mms.MESSAGE_BOX_SENT]).
     *   If null, defaults to FROM (type=137) for backward compatibility.
     */
    fun resolveAddress(context: Context, mmsId: Long, msgBox: Int? = null): String {
        val uri = Uri.withAppendedPath(Uri.parse("content://mms/$mmsId"), "addr")
        val cursor = context.contentResolver.query(
            uri,
            arrayOf("address", "type"),
            null,
            null,
            null,
        ) ?: return ""

        val targetType = if (msgBox == Telephony.Mms.MESSAGE_BOX_INBOX) 137 else 151
        // inbound  → FROM  (type=137) → the contact who sent the MMS
        // outbound → TO    (type=151) → the contact who receives the MMS

        return cursor.use { c ->
            while (c.moveToNext()) {
                val type = c.getInt(c.getColumnIndexOrThrow("type"))
                if (type == targetType) {
                    return@use c.getString(c.getColumnIndexOrThrow("address")) ?: ""
                }
            }
            ""
        }
    }

    fun resolveBothAddresses(context: Context, mmsId: Long): Pair<String, String> {
        val uri = Uri.withAppendedPath(Uri.parse("content://mms/$mmsId"), "addr")
        val cursor = context.contentResolver.query(
            uri,
            arrayOf("address", "type"),
            null,
            null,
            null,
        ) ?: return "" to ""

        return cursor.use { c ->
            var from = ""
            var to = ""
            while (c.moveToNext()) {
                val type = c.getInt(c.getColumnIndexOrThrow("type"))
                val address = c.getString(c.getColumnIndexOrThrow("address")) ?: ""
                when (type) {
                    137 -> from = address
                    151 -> to = address
                }
            }
            from to to
        }
    }
}
