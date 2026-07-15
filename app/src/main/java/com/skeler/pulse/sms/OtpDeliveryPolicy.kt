package com.skeler.pulse.sms

internal enum class OtpDelivery {
    Snackbar,
    NotificationCopyAction,
}

internal object OtpDeliveryPolicy {
    fun deliveryFor(isAppForeground: Boolean): OtpDelivery =
        if (isAppForeground) OtpDelivery.Snackbar else OtpDelivery.NotificationCopyAction
}
