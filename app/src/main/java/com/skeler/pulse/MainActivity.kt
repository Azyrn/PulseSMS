package com.skeler.pulse

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.skeler.pulse.design.theme.SerafinaAppTheme
import com.skeler.pulse.design.theme.SerafinaThemeViewModel
import com.skeler.pulse.ui.PulseAppShell
import com.skeler.pulse.ui.RealSmsViewModel

class MainActivity : ComponentActivity() {

    private val smsRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* Result handled — app is now default or user declined */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as PulseApplication).appContainer
        val themeViewModel = ViewModelProvider(this)[SerafinaThemeViewModel::class.java]
        val realSmsViewModel = ViewModelProvider(
            this,
            appContainer.realSmsViewModelFactory(),
        )[RealSmsViewModel::class.java]

        setContent {
            val themeState by themeViewModel.state.collectAsState()

            SerafinaAppTheme(
                themeState = themeState,
                reduceMotion = themeState.reduceMotion,
            ) {
                val inboxState by realSmsViewModel.inboxState.collectAsState()
                val conversationState by realSmsViewModel.conversationState.collectAsState()

                PulseAppShell(
                    inboxState = inboxState,
                    conversationState = conversationState,
                    onOpenConversation = realSmsViewModel::openConversation,
                    onSendMessage = realSmsViewModel::sendMessage,
                    onToggleImportantMessage = realSmsViewModel::toggleImportantMessage,
                    themeViewModel = themeViewModel,
                    onRequestDefaultSms = { requestDefaultSmsApp() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    /**
     * Requests the user to set Pulse as the default SMS app.
     * If already default, opens system default apps settings so user can see status.
     */
    @Suppress("DEPRECATION")
    private fun requestDefaultSmsApp() {
        val currentDefault = Telephony.Sms.getDefaultSmsPackage(this)

        if (currentDefault == packageName) {
            // Already default — open system default apps settings
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            startActivity(intent)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                smsRoleLauncher.launch(intent)
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            }
            smsRoleLauncher.launch(intent)
        }
    }

    companion object {
        const val EXTRA_CONVERSATION_ID: String = "extra_conversation_id"
        const val DEFAULT_CONVERSATION_ID: String = "business-primary"
    }
}
