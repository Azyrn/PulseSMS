package com.skeler.pulse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import android.os.Build

class MainActivityPermissionFlowTest {

    @Test
    fun `opens new chat after permission result when request was pending and contacts granted`() {
        assertTrue(
            shouldOpenNewChatAfterPermissionResult(
                requestedNewChatOpen = true,
                hasContactPermission = true,
            ),
        )
    }

    @Test
    fun `does not open new chat after permission result when request was not pending`() {
        assertFalse(
            shouldOpenNewChatAfterPermissionResult(
                requestedNewChatOpen = false,
                hasContactPermission = true,
            ),
        )
    }

    @Test
    fun `does not open new chat after permission result when contacts remain denied`() {
        assertFalse(
            shouldOpenNewChatAfterPermissionResult(
                requestedNewChatOpen = true,
                hasContactPermission = false,
            ),
        )
    }

    @Test
    fun `handles launch request only when inbox access is ready`() {
        assertTrue(
            shouldHandleLaunchRequest(
                launchRequest = PulseLaunchRequest(conversationAddress = "+15551234567"),
                accessState = InboxAccessState(permissionDenied = false, isDefaultSmsApp = true),
            ),
        )
        assertFalse(
            shouldHandleLaunchRequest(
                launchRequest = PulseLaunchRequest(conversationAddress = "+15551234567"),
                accessState = InboxAccessState(permissionDenied = true, isDefaultSmsApp = true),
            ),
        )
        assertFalse(
            shouldHandleLaunchRequest(
                launchRequest = PulseLaunchRequest(conversationAddress = "+15551234567"),
                accessState = InboxAccessState(permissionDenied = false, isDefaultSmsApp = false),
            ),
        )
    }

    @Test
    fun `handles open new chat request only when request is new and access is ready`() {
        val readyState = InboxAccessState(permissionDenied = false, isDefaultSmsApp = true)
        val blockedState = InboxAccessState(permissionDenied = true, isDefaultSmsApp = true)

        assertTrue(
            shouldHandleOpenNewChatRequest(
                requestKey = 3,
                lastHandledRequestKey = 2,
                accessState = readyState,
            ),
        )
        assertFalse(
            shouldHandleOpenNewChatRequest(
                requestKey = 3,
                lastHandledRequestKey = 3,
                accessState = readyState,
            ),
        )
        assertFalse(
            shouldHandleOpenNewChatRequest(
                requestKey = 3,
                lastHandledRequestKey = 2,
                accessState = blockedState,
            ),
        )
    }

    @Test
    fun `inbox access state is ready only when permission is granted and app is default sms`() {
        assertTrue(InboxAccessState(permissionDenied = false, isDefaultSmsApp = true).isReady)
        assertFalse(InboxAccessState(permissionDenied = true, isDefaultSmsApp = true).isReady)
        assertFalse(InboxAccessState(permissionDenied = false, isDefaultSmsApp = false).isReady)
        assertEquals(false, InboxAccessState(permissionDenied = true, isDefaultSmsApp = false).isReady)
    }

    @Test
    fun `default sms access is granted on android q and above when sms role is held`() {
        assertTrue(
            isDefaultSmsApp(
                packageName = "com.skeler.pulse",
                telephonyDefaultPackage = null,
                sdkInt = Build.VERSION_CODES.Q,
                smsRoleHeld = true,
            ),
        )
    }

    @Test
    fun `default sms access remains blocked below android q when telephony default does not match`() {
        assertFalse(
            isDefaultSmsApp(
                packageName = "com.skeler.pulse",
                telephonyDefaultPackage = "com.android.messaging",
                sdkInt = Build.VERSION_CODES.P,
                smsRoleHeld = true,
            ),
        )
    }
}
