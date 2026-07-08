package com.skeler.pulse.sms

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.skeler.pulse.MainActivity

/**
 * Activity that handles `ACTION_SENDTO` with `sms:`, `smsto:`, `mms:`, `mmsto:` URI schemes.
 *
 * This is required by Android to make the app eligible as the default SMS handler.
 * When another app wants to compose an SMS (e.g., tapping a phone number → "Send SMS"),
 * the system routes the intent here.
 *
 * Extracts the recipient number and optional body, then delegates to [MainActivity].
 */
class ComposeSmsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent?.data
        val recipient = extractRecipient(data)
        val body = intent?.getStringExtra("sms_body")
            ?: intent?.getStringExtra(Intent.EXTRA_TEXT)
            ?: safeQueryParameter(data, "body")
            ?: safeQueryParameter(data, "sms_body")
            ?: ""

        val launchIntent = MainActivity.createLaunchIntent(
            context = this,
            conversationAddress = recipient,
            draftBody = body,
        )
        startActivity(launchIntent)
        finish()
    }

    /**
     * Extracts the phone number from `sms:+1234567890` or `smsto:+1234567890` URIs.
     */
    private fun safeQueryParameter(uri: Uri?, key: String): String? {
        if (uri?.isHierarchical != true) return null
        return try {
            uri.getQueryParameter(key)
        } catch (_: UnsupportedOperationException) {
            null
        }
    }

    private fun extractRecipient(uri: Uri?): String {
        if (uri == null) return ""
        return uri.schemeSpecificPart
            ?.substringBefore("?")
            ?.split(",", ";")
            ?.joinToString(separator = ",") { recipient ->
                recipient.replace("-", "").trim()
            }
            ?.trim(',')
            .orEmpty()
    }

}
