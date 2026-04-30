package com.skeler.pulse

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.skeler.pulse.contact.displayNameFor
import com.skeler.pulse.design.theme.SerafinaAppTheme
import com.skeler.pulse.design.theme.SerafinaThemeMode
import com.skeler.pulse.design.theme.SerafinaThemeViewModel
import com.skeler.pulse.ui.PulseAppShell
import com.skeler.pulse.ui.RealSmsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PulseLaunchRequest(
    val conversationAddress: String = "",
    val conversationTitle: String = "",
    val draftBody: String = "",
)

data class InboxAccessState(
    val permissionDenied: Boolean = false,
    val isDefaultSmsApp: Boolean = true,
) {
    val isReady: Boolean get() = !permissionDenied && isDefaultSmsApp
}

private const val NEW_CHAT_PERMISSION_REQUEST_NONE = 0
private const val NEW_CHAT_PERMISSION_REQUEST_PENDING_OPEN = 1
private const val DEFAULT_SMS_RECONCILE_ATTEMPTS = 5
private const val DEFAULT_SMS_RECONCILE_DELAY_MILLIS = 250L

internal fun requiredCorePermissions(sdkInt: Int): List<String> = buildList {
    add(Manifest.permission.READ_SMS)
    add(Manifest.permission.SEND_SMS)
    add(Manifest.permission.RECEIVE_SMS)
    if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}

internal fun requiredNewChatPermissions(): List<String> = listOf(
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.READ_PHONE_STATE,
)

internal fun missingPermissions(
    context: Context,
    permissions: List<String>,
): List<String> = permissions.filter {
    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
}

internal fun shouldOpenNewChatAfterPermissionResult(
    requestedNewChatOpen: Boolean,
    hasContactPermission: Boolean,
): Boolean = requestedNewChatOpen && hasContactPermission

internal fun shouldHandleLaunchRequest(
    launchRequest: PulseLaunchRequest?,
    accessState: InboxAccessState,
): Boolean = launchRequest != null && accessState.isReady

internal fun shouldHandleOpenNewChatRequest(
    requestKey: Int,
    lastHandledRequestKey: Int,
    accessState: InboxAccessState,
): Boolean = requestKey > lastHandledRequestKey && accessState.isReady

internal fun buildPulseLaunchRequestOrNull(
    conversationAddress: String?,
    draftBody: String?,
): PulseLaunchRequest? {
    val normalizedConversationAddress = conversationAddress.orEmpty().trim()
    val normalizedDraftBody = draftBody.orEmpty().trim()
    return if (normalizedConversationAddress.isBlank() && normalizedDraftBody.isBlank()) {
        null
    } else {
        PulseLaunchRequest(
            conversationAddress = normalizedConversationAddress,
            draftBody = normalizedDraftBody,
        )
    }
}

internal fun Context.toPulseLaunchRequestOrNull(
    conversationAddress: String?,
    draftBody: String?,
): PulseLaunchRequest? {
    val request = buildPulseLaunchRequestOrNull(
        conversationAddress = conversationAddress,
        draftBody = draftBody,
    ) ?: return null
    return request.copy(
        conversationTitle = request.conversationAddress
            .takeIf(String::isNotBlank)
            ?.let { displayNameFor(this, it) }
            .orEmpty(),
    )
}

internal fun Intent?.toPulseLaunchRequestOrNull(context: Context): PulseLaunchRequest? {
    if (this == null) return null
    return context.toPulseLaunchRequestOrNull(
        conversationAddress = getStringExtra(MainActivity.EXTRA_CONVERSATION_ADDRESS),
        draftBody = getStringExtra(MainActivity.EXTRA_COMPOSE_BODY),
    )
}

internal fun resolveInboxAccessState(
    context: Context,
    sdkInt: Int,
    packageName: String,
): InboxAccessState = InboxAccessState(
    permissionDenied = missingPermissions(context, requiredCorePermissions(sdkInt)).isNotEmpty(),
    isDefaultSmsApp = isDefaultSmsApp(
        context = context,
        sdkInt = sdkInt,
        packageName = packageName,
    ),
)

internal fun isDefaultSmsApp(
    packageName: String,
    telephonyDefaultPackage: String?,
    sdkInt: Int,
    smsRoleHeld: Boolean,
): Boolean {
    if (telephonyDefaultPackage == packageName) {
        return true
    }
    if (sdkInt < Build.VERSION_CODES.Q) {
        return false
    }
    return smsRoleHeld
}

internal fun isDefaultSmsApp(
    context: Context,
    sdkInt: Int,
    packageName: String,
): Boolean {
    val telephonyDefaultPackage = Telephony.Sms.getDefaultSmsPackage(context)
    val smsRoleHeld = if (sdkInt >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        roleManager != null &&
            roleManager.isRoleAvailable(RoleManager.ROLE_SMS) &&
            roleManager.isRoleHeld(RoleManager.ROLE_SMS)
    } else {
        false
    }
    return isDefaultSmsApp(
        packageName = packageName,
        telephonyDefaultPackage = telephonyDefaultPackage,
        sdkInt = sdkInt,
        smsRoleHeld = smsRoleHeld,
    )
}

class MainActivity : ComponentActivity() {

    private lateinit var realSmsViewModel: RealSmsViewModel
    private var inboxAccessReconcileJob: Job? = null

    private val launchRequestState = mutableStateOf<PulseLaunchRequest?>(null)
    private val openNewChatRequestKeyState = mutableIntStateOf(0)
    private val newChatPermissionRequestState = mutableIntStateOf(NEW_CHAT_PERMISSION_REQUEST_NONE)

    private val smsRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshInboxAccessState(retryIfDefaultSmsPending = true)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshInboxAccessState()
        val requestedNewChatOpen = newChatPermissionRequestState.intValue == NEW_CHAT_PERMISSION_REQUEST_PENDING_OPEN
        newChatPermissionRequestState.intValue = NEW_CHAT_PERMISSION_REQUEST_NONE
        if (shouldOpenNewChatAfterPermissionResult(
                requestedNewChatOpen = requestedNewChatOpen,
                hasContactPermission = missingPermissions(
                    context = this,
                    permissions = listOf(Manifest.permission.READ_CONTACTS),
                ).isEmpty(),
            )
        ) {
            openNewChatRequestKeyState.intValue += 1
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        requestRequiredPermissions()
        launchRequestState.value = intent.toPulseLaunchRequestOrNull(this)

        val appContainer = (application as PulseApplication).appContainer
        val themeViewModel = ViewModelProvider(this)[SerafinaThemeViewModel::class.java]
        realSmsViewModel = ViewModelProvider(
            this,
            appContainer.realSmsViewModelFactory(),
        )[RealSmsViewModel::class.java]
        refreshInboxAccessState()

        setContent {
            val themeState by themeViewModel.state.collectAsState()
            val launchRequest = launchRequestState.value
            val accessState by realSmsViewModel.inboxState.collectAsState()

            SerafinaAppTheme(
                themeState = themeState,
                reduceMotion = themeState.reduceMotion,
            ) {
                val isDarkMode = when (themeState.themeMode) {
                    SerafinaThemeMode.System -> isSystemInDarkTheme()
                    SerafinaThemeMode.Light -> false
                    SerafinaThemeMode.Dark -> true
                }
                val view = LocalView.current
                SideEffect {
                    val controller = WindowInsetsControllerCompat(window, view)
                    controller.isAppearanceLightStatusBars = !isDarkMode
                    controller.isAppearanceLightNavigationBars = !isDarkMode
                }

                PulseAppShell(
                    smsViewModel = realSmsViewModel,
                    launchRequest = launchRequest,
                    openNewChatRequestKey = openNewChatRequestKeyState.intValue,
                    accessState = InboxAccessState(
                        permissionDenied = accessState.permissionDenied,
                        isDefaultSmsApp = accessState.isDefaultSmsApp,
                    ),
                    onLaunchRequestConsumed = { launchRequestState.value = null },
                    onRequestNewChat = { requestNewChatPermissionsAndOpen() },
                    onRequestSmsPermissions = { requestRequiredPermissions() },
                    onOpenConversation = { address, threadId ->
                        realSmsViewModel.openConversation(address, threadId)
                    },
                    onSendMessage = { address, body, subscriptionId ->
                        realSmsViewModel.sendMessage(address, body, subscriptionId)
                    },
                    themeViewModel = themeViewModel,
                    onRequestDefaultSms = { requestDefaultSmsApp() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchRequestState.value = intent.toPulseLaunchRequestOrNull(this)
    }

    override fun onResume() {
        super.onResume()
        if (::realSmsViewModel.isInitialized) {
            refreshInboxAccessState(retryIfDefaultSmsPending = true)
        }
    }

    /**
     * Request all runtime permissions needed for SMS functionality.
     * Only requests permissions that haven't been granted yet.
     */
    private fun requestRequiredPermissions() {
        val missing = missingPermissions(this, requiredCorePermissions(Build.VERSION.SDK_INT))
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun requestNewChatPermissionsAndOpen() {
        val missing = missingPermissions(this, requiredNewChatPermissions())
        if (missing.isEmpty()) {
            openNewChatRequestKeyState.intValue += 1
            return
        }
        newChatPermissionRequestState.intValue = NEW_CHAT_PERMISSION_REQUEST_PENDING_OPEN
        permissionLauncher.launch(missing.toTypedArray())
    }

    /**
     * Requests the user to set Pulse as the default SMS app.
     * If already default, opens system default apps settings so user can see status.
     */
    @Suppress("DEPRECATION")
    private fun requestDefaultSmsApp() {
        val currentDefault = Telephony.Sms.getDefaultSmsPackage(this)

        if (currentDefault == packageName) {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            startActivity(intent)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (
                roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_SMS) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_SMS)
            ) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                smsRoleLauncher.launch(intent)
                return
            }
            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            }
            smsRoleLauncher.launch(intent)
        }
    }

    private fun refreshInboxAccessState(retryIfDefaultSmsPending: Boolean = false) {
        if (!::realSmsViewModel.isInitialized) return
        val accessState = resolveInboxAccessState(
            context = this,
            sdkInt = Build.VERSION.SDK_INT,
            packageName = packageName,
        )
        realSmsViewModel.updateInboxAccessState(accessState)

        val shouldRetry = retryIfDefaultSmsPending &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !accessState.permissionDenied &&
            !accessState.isDefaultSmsApp
        if (!shouldRetry) {
            inboxAccessReconcileJob?.cancel()
            inboxAccessReconcileJob = null
            return
        }

        inboxAccessReconcileJob?.cancel()
        inboxAccessReconcileJob = lifecycleScope.launch {
            repeat(DEFAULT_SMS_RECONCILE_ATTEMPTS - 1) {
                delay(DEFAULT_SMS_RECONCILE_DELAY_MILLIS)
                val reconciledAccessState = resolveInboxAccessState(
                    context = this@MainActivity,
                    sdkInt = Build.VERSION.SDK_INT,
                    packageName = packageName,
                )
                realSmsViewModel.updateInboxAccessState(reconciledAccessState)
                if (reconciledAccessState.permissionDenied || reconciledAccessState.isDefaultSmsApp) {
                    inboxAccessReconcileJob = null
                    return@launch
                }
            }
            inboxAccessReconcileJob = null
        }
    }

    companion object {
        const val EXTRA_CONVERSATION_ID: String = "extra_conversation_id"
        const val EXTRA_CONVERSATION_ADDRESS: String = "extra_conversation_address"
        const val EXTRA_COMPOSE_BODY: String = "extra_compose_body"
        const val DEFAULT_CONVERSATION_ID: String = "business-primary"

        fun createLaunchIntent(
            context: Context,
            conversationAddress: String? = null,
            draftBody: String? = null,
        ): Intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            conversationAddress
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?.let { putExtra(EXTRA_CONVERSATION_ADDRESS, it) }
            draftBody
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?.let { putExtra(EXTRA_COMPOSE_BODY, it) }
        }
    }
}
