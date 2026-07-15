package com.skeler.pulse.sms

import org.junit.Assert.assertEquals
import org.junit.Test

class OtpDeliveryPolicyTest {

    @Test
    fun should_show_snackbar_when_app_is_foreground() {
        val delivery = OtpDeliveryPolicy.deliveryFor(isAppForeground = true)

        assertEquals(OtpDelivery.Snackbar, delivery)
    }

    @Test
    fun should_show_notification_action_when_app_is_background() {
        val delivery = OtpDeliveryPolicy.deliveryFor(isAppForeground = false)

        assertEquals(OtpDelivery.NotificationCopyAction, delivery)
    }
}
