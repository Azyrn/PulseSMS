package com.skeler.pulse.sms

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
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
            val loaded = loadRecentContacts(context, 8)
            val target = QuickComposeNotificationManager.getTargetNumber(context)
            val normalizedTarget = target?.let { normalizePhoneNumberRaw(it) }
            val match = if (normalizedTarget != null) {
                loaded.firstOrNull { normalizePhoneNumberRaw(it.phoneNumber) == normalizedTarget }
                    ?: searchContacts(context, normalizedTarget).firstOrNull {
                        normalizePhoneNumberRaw(it.phoneNumber) == normalizedTarget
                    }
                    ?: ContactMatch(displayNameFor(context, normalizedTarget), normalizedTarget)
            } else null
            Pair(loaded, match)
        }
        recentContacts.addAll(contacts)
        number?.let {
            selectedContact = it
            contactQuery = it.displayName
        }
    }

    val contactNumber = remember(selectedContact, contactQuery) {
        selectedContact?.phoneNumber
            ?: contactQuery.takeIf { it.isNotBlank() }
                ?.let { normalizePhoneNumberRaw(it) }
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
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.new_chat_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (recentContacts.isNotEmpty() && contactQuery.isBlank() && selectedContact == null) {
                    Text(
                        text = stringResource(R.string.quick_compose_recent_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp).fillMaxWidth(),
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
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
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
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
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedContact != null) {
                    val contact = selectedContact!!
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        tonalElevation = 2.dp,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .size(18.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = contact.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 200.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    selectedContact = null
                                    contactQuery = ""
                                },
                                modifier = Modifier.size(20.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.action_close),
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = contact.phoneNumber,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                    )
                } else {
                    OutlinedTextField(
                        value = contactQuery,
                        onValueChange = { contactQuery = it },
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
                        visible = suggestions.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Column {
                            Spacer(Modifier.height(4.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
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

                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text(stringResource(R.string.quick_compose_message_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp)
                        .focusRequester(messageFocusRequester),
                    maxLines = 6,
                    trailingIcon = {
                        val canSend = contactNumber != null && messageText.isNotBlank()
                        IconButton(
                            onClick = {
                                if (canSend && !isSending) {
                                    isSending = true
                                    keyboardController?.hide()
                                    onSend(contactNumber, messageText.trim())
                                }
                            },
                            enabled = canSend && !isSending,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Send,
                                contentDescription = stringResource(R.string.quick_compose_send_action),
                                tint = if (canSend && !isSending)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            )
                        }
                    },
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

                if (contactNumber != null) {
                    Spacer(Modifier.height(2.dp))
                    TextButton(
                        onClick = {
                            keyboardController?.hide()
                            val intent = com.skeler.pulse.MainActivity.createLaunchIntent(
                                context = context,
                                conversationAddress = contactNumber,
                                draftBody = messageText.trim().ifBlank { null },
                            )
                            context.startActivity(intent)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.quick_compose_open_conversation),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
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
