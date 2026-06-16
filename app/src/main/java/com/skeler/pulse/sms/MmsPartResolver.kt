package com.skeler.pulse.sms

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

internal object MmsPartResolver {

    private const val TEXT_PLAIN = "text/plain"
    private const val TEXT_HTML = "text/html"
    private const val TAG = "MmsPartResolver"

    data class MmsPartsResult(
        val textBody: String?,
        val attachmentUri: Uri?,
        val attachmentMimeType: String? = null,
    )

    fun resolveParts(context: Context, mmsId: Long): MmsPartsResult {
        val parts = queryParts(context, mmsId) ?: return MmsPartsResult(null, null)

        var textBody: String? = null
        var attachmentUri: Uri? = null
        var attachmentMimeType: String? = null

        for (entry in parts) {
            when (entry.mimeType) {
                TEXT_PLAIN, TEXT_HTML -> if (textBody == null) {
                    textBody = readPartContent(context, entry.id)
                }
                "application/smil" -> { /* skip SMIL manifest */ }
                else -> if (attachmentUri == null) {
                    val cacheFile = File(context.cacheDir, "mms_parts/${entry.id}")
                    attachmentUri = if (cacheFile.exists()) {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.mmsfileprovider",
                            cacheFile,
                        )
                    } else {
                        Uri.parse("content://mms/part/${entry.id}")
                    }
                    attachmentMimeType = entry.mimeType
                    Log.i(TAG, "Attachment URI: $attachmentUri (${entry.mimeType})")
                }
            }
        }

        return MmsPartsResult(
            textBody = textBody,
            attachmentUri = attachmentUri,
            attachmentMimeType = attachmentMimeType,
        )
    }

    fun resolveTextBody(context: Context, mmsId: Long): String? {
        val parts = queryParts(context, mmsId) ?: return null
        return parts.firstOrNull { entry ->
            entry.mimeType == TEXT_PLAIN || entry.mimeType == TEXT_HTML
        }?.let { entry ->
            readPartContent(context, entry.id)
        }
    }

    fun resolveFirstAttachmentUri(context: Context, mmsId: Long): Uri? {
        val parts = queryParts(context, mmsId) ?: return null
        val entry = parts.firstOrNull { entry ->
            entry.mimeType != TEXT_PLAIN &&
                entry.mimeType != TEXT_HTML &&
                entry.mimeType != "application/smil"
        } ?: return null

        val cacheFile = File(context.cacheDir, "mms_parts/${entry.id}")
        if (cacheFile.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.mmsfileprovider",
                cacheFile,
            )
            Log.i(TAG, "Attachment URI (cached): $uri (${entry.mimeType})")
            return uri
        }

        Log.i(TAG, "Attachment URI (mms provider): content://mms/part/${entry.id} (${entry.mimeType})")
        return Uri.parse("content://mms/part/${entry.id}")
    }

    private fun queryParts(context: Context, mmsId: Long): List<PartEntry>? {
        val uri = Uri.parse("content://mms/$mmsId/part")
        Log.i(TAG, "Querying parts from $uri")
        val cursor = context.contentResolver.query(
            uri,
            arrayOf("_id", "ct", "name"),
            null,
            null,
            null,
        ) ?: return null.also { Log.w(TAG, "Parts query returned null for mmsId=$mmsId") }

        return cursor.use { c ->
            val entries = mutableListOf<PartEntry>()
            while (c.moveToNext()) {
                val id = c.getLong(c.getColumnIndexOrThrow("_id"))
                val mimeType = c.getString(c.getColumnIndexOrThrow("ct")) ?: ""
                Log.i(TAG, "Found part: id=$id ct=$mimeType for mmsId=$mmsId")
                entries.add(PartEntry(id, mimeType))
            }
            if (entries.isEmpty()) Log.w(TAG, "No entries found for mmsId=$mmsId")
            entries
        }
    }

    private fun readPartContent(context: Context, partId: Long): String? {
        val uri = Uri.parse("content://mms/part/$partId")
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            }
        } catch (e: Exception) {
            null
        }
    }

    private data class PartEntry(
        val id: Long,
        val mimeType: String,
    )
}
