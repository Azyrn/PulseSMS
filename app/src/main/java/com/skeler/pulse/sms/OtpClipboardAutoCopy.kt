package com.skeler.pulse.sms

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking

internal object OtpClipboardAutoCopy {
    private const val LOG_TAG = "OtpClipboardAutoCopy"
    private const val CLIPBOARD_LABEL = "Verification code"

    fun codeToCopy(body: String, isEnabled: Boolean): String? {
        if (!isEnabled) return null
        return OtpCodeExtractor.extractCode(body)
    }

    fun copyIncomingCodeIfEnabled(context: Context, body: String): Boolean {
        val appContext = context.applicationContext
        val isEnabled = runBlocking {
            MessageAutomationPreferences(appContext).isAutoCopyOtpCodesEnabled()
        }
        val code = codeToCopy(body = body, isEnabled = isEnabled) ?: return false
        val clipboardManager = appContext.getSystemService(ClipboardManager::class.java) ?: return false

        return runCatching {
            clipboardManager.setPrimaryClip(ClipData.newPlainText(CLIPBOARD_LABEL, code))
            true
        }.getOrElse { error ->
            Log.w(LOG_TAG, "Failed to copy incoming verification code to clipboard", error)
            false
        }
    }
}
