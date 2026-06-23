package com.skeler.pulse.sms

import android.content.Context
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.skeler.pulse.R
import com.skeler.pulse.contact.displayNameFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

private data class ContactMatch(
    val displayName: String,
    val phoneNumber: String,
)

class QuickComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuickComposeSheet(
                onDismiss = { finish() },
                onSend = { address, message ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            SystemSmsSender(this@QuickComposeActivity, Dispatchers.IO)
                                .sendSmsFireAndForget(address, message)
                            withContext(Dispatchers.Main) {
                                Toast
                                    .makeText(
                                        this@QuickComposeActivity,
                                        R.string.quick_compose_sent_toast,
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                            }
                        } catch (e: Exception) {
                            Log.e("QuickComposeActivity", "send failed", e)
                            withContext(Dispatchers.Main) {
                                Toast
                                    .makeText(
                                        this@QuickComposeActivity,
                                        R.string.quick_compose_failed_toast,
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                if (!isFinishing && !isDestroyed) finish()
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun QuickComposeSheet(
    onDismiss: () -> Unit,
    onSend: (address: String, message: String) -> Unit,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var contactQuery by remember { mutableStateOf("") }
    var selectedContact by remember { mutableStateOf<ContactMatch?>(null) }
    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val suggestions = remember { mutableStateListOf<ContactMatch>() }
    val recentContacts = remember { mutableStateListOf<ContactMatch>() }
    val messageFocusRequester = remember { FocusRequester() }

    LaunchedEffect(selectedContact) {
        if (selectedContact != null) {
            messageFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(Unit) {
        val (contacts, number) = withContext(Dispatchers.IO) {
            loadRecentContacts(context, 8) to
                QuickComposeNotificationManager.getTargetNumber(context)
        }
        recentContacts.addAll(contacts)
        number?.let { contactQuery = it }
    }

    val contactNumber = remember(selectedContact, contactQuery) {
        selectedContact?.phoneNumber
            ?: contactQuery.takeIf { it.isNotBlank() }
                ?.let { normalizePhoneNumberRaw(it) }
    }

    val displayText = remember(selectedContact, contactQuery) {
        if (selectedContact != null) {
            "${selectedContact!!.displayName} <${selectedContact!!.phoneNumber}>"
        } else {
            contactQuery
        }
    }

    LaunchedEffect(contactQuery) {
        if (contactQuery.isBlank() || selectedContact != null) {
            suggestions.clear()
            return@LaunchedEffect
        }
        delay(300.milliseconds)
        val query = contactQuery
        val matches = withContext(Dispatchers.IO) {
            searchContacts(context, query)
        }
        if (query == contactQuery) {
            suggestions.clear()
            suggestions.addAll(matches)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    keyboardController?.hide()
                    onDismiss()
                },
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { },
                ),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 24.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.new_chat_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))

                if (recentContacts.isNotEmpty() && contactQuery.isBlank() && selectedContact == null) {
                    Text(
                        text = stringResource(R.string.quick_compose_recent_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(recentContacts) { contact ->
                            Surface(
                                onClick = {
                                    selectedContact = contact
                                    contactQuery = contact.displayName
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                tonalElevation = 2.dp,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Rounded.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = contact.displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = displayText,
                        onValueChange = { newValue ->
                            if (selectedContact != null) {
                                selectedContact = null
                                contactQuery = if (newValue.length > displayText.length) {
                                    newValue.last().toString()
                                } else {
                                    ""
                                }
                            } else {
                                contactQuery = newValue
                            }
                        },
                        label = { Text(stringResource(R.string.quick_compose_to_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Next,
                        ),
                        enabled = !isSending,
                    )

                    AnimatedVisibility(
                        visible = suggestions.isNotEmpty() && selectedContact == null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Column {
                            Spacer(Modifier.height(4.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            ) {
                                LazyColumn {
                                    items(suggestions) { contact ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedContact = contact
                                                    contactQuery = contact.displayName
                                                    suggestions.clear()
                                                }
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                        ) {
                                            Column {
                                                Text(
                                                    text = contact.displayName,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                )
                                                Text(
                                                    text = contact.phoneNumber,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text(stringResource(R.string.quick_compose_message_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp)
                        .focusRequester(messageFocusRequester),
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (contactNumber != null && messageText.isNotBlank() && !isSending) {
                                isSending = true
                                keyboardController?.hide()
                                onSend(contactNumber, messageText.trim())
                            }
                        },
                    ),
                    enabled = !isSending,
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        keyboardController?.hide()
                        contactNumber?.let { addr ->
                            isSending = true
                            onSend(addr, messageText.trim())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = contactNumber != null && messageText.isNotBlank() && !isSending,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.quick_compose_send_action))
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

private fun searchContacts(context: Context, query: String): List<ContactMatch> {
    if (query.isBlank()) return emptyList()
    val results = mutableListOf<ContactMatch>()
    val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
    val selectionArgs = arrayOf("%$query%", "%$query%")
    try {
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            selection,
            selectionArgs,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC LIMIT 20",
        )?.use { cursor ->
            val seen = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                val name = cursor.getString(0)?.trim() ?: continue
                val number = cursor.getString(1)?.trim() ?: continue
                if (number.isBlank()) continue
                val key = "$name:$number"
                if (key !in seen) {
                    seen.add(key)
                    results.add(ContactMatch(name, number))
                }
            }
        }
    } catch (_: SecurityException) {
    }
    return results
}

private fun loadRecentContacts(context: Context, limit: Int): List<ContactMatch> {
    val results = mutableListOf<ContactMatch>()
    val seen = mutableSetOf<String>()
    val uri = Telephony.Sms.CONTENT_URI.buildUpon()
        .appendQueryParameter("limit", "100")
        .build()
    try {
        context.contentResolver.query(
            uri,
            arrayOf(Telephony.Sms.ADDRESS),
            null,
            null,
            "${Telephony.Sms.DATE} DESC",
        )?.use { cursor ->
            while (cursor.moveToNext() && results.size < limit) {
                val address = cursor.getString(0)?.trim() ?: continue
                if (address.isBlank()) continue
                val normalized = normalizePhoneNumberRaw(address) ?: continue
                if (normalized !in seen) {
                    seen.add(normalized)
                    val displayName = displayNameFor(context, address)
                    results.add(ContactMatch(displayName, address))
                }
            }
        }
    } catch (_: Exception) {
    }
    return results
}

private fun normalizePhoneNumberRaw(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.startsWith("+")) {
        if (trimmed.count { it.isDigit() } < 8) return null
        return trimmed
    }
    val cleaned = trimmed.replace(Regex("[^\\d+]"), "")
    if (cleaned.count { it.isDigit() } < 8) return null
    return cleaned
}
