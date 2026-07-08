package com.skeler.pulse.sms

import android.content.Context
import android.telephony.TelephonyManager

internal object MyPhoneNumberProvider {

    @Suppress("DEPRECATION")
    fun detect(context: Context): String? {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.line1Number?.filter { it.isDigit() || it == '+' }
            ?.takeIf { it.isNotBlank() }
    }

    fun isMyNumber(context: Context, address: String): Boolean {
        val myDigits = detect(context)?.filter(Char::isDigit) ?: return false
        val theirDigits = address.filter(Char::isDigit)
        if (myDigits == theirDigits) return true
        val mySuffix = myDigits.takeLast(9)
        val theirSuffix = theirDigits.takeLast(9)
        return mySuffix == theirSuffix
    }
}
