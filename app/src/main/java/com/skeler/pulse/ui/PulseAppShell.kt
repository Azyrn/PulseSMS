@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.skeler.pulse.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skeler.pulse.BuildConfig
import com.skeler.pulse.contracts.protocol.ProtocolMode
import com.skeler.pulse.contracts.security.KeyStoreCapability
import com.skeler.pulse.design.component.SerafinaAvatar
import com.skeler.pulse.design.theme.SerafinaPalette
import com.skeler.pulse.design.theme.SerafinaThemeViewModel
import com.skeler.pulse.sms.SmsThread
import com.skeler.pulse.sms.SystemSms
import com.skeler.pulse.ui.settings.SettingsViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class PulseScreen { Inbox, Conversation, Settings }

private enum class InboxFilter(val label: String) {
    All("All"), Personal("Personal"), Business("Business"), OTP("OTP"),
}

@Composable
fun PulseAppShell(
    inboxState: RealInboxState,
    conversationState: RealConversationState,
    onOpenConversation: (String) -> Unit,
    onSendMessage: (String, String) -> Unit,
    themeViewModel: SerafinaThemeViewModel,
    settingsViewModel: SettingsViewModel,
    onRequestDefaultSms: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var currentScreen by rememberSaveable { mutableStateOf(PulseScreen.Inbox) }
    var activeAddress by rememberSaveable { mutableStateOf("") }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                val dur = 250
                when {
                    targetState == PulseScreen.Conversation && initialState == PulseScreen.Inbox ->
                        (slideInHorizontally(tween(dur)) { it / 4 } + fadeIn(tween(dur)))
                            .togetherWith(slideOutHorizontally(tween(dur)) { -it / 4 } + fadeOut(tween(dur)))
                    targetState == PulseScreen.Inbox && initialState == PulseScreen.Conversation ->
                        (slideInHorizontally(tween(dur)) { -it / 4 } + fadeIn(tween(dur)))
                            .togetherWith(slideOutHorizontally(tween(dur)) { it / 4 } + fadeOut(tween(dur)))
                    else -> fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                }
            },
            label = "screen_transition",
        ) { screen ->
            when (screen) {
                PulseScreen.Inbox -> RealInboxScreen(
                    threads = inboxState.threads,
                    loading = inboxState.loading,
                    onOpenConversation = { address ->
                        activeAddress = address
                        onOpenConversation(address)
                        currentScreen = PulseScreen.Conversation
                    },
                    onOpenSettings = { currentScreen = PulseScreen.Settings },
                )
                PulseScreen.Conversation -> RealConversationScreen(
                    address = activeAddress,
                    messages = conversationState.messages,
                    onBack = { currentScreen = PulseScreen.Inbox },
                    onSend = { body -> onSendMessage(activeAddress, body) },
                )
                PulseScreen.Settings -> SettingsScreen(
                    themeViewModel = themeViewModel,
                    settingsViewModel = settingsViewModel,
                    onBack = { currentScreen = PulseScreen.Inbox },
                    onRequestDefaultSms = onRequestDefaultSms,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// REAL SMS INBOX
// ═══════════════════════════════════════════════════════════

@Composable
private fun RealInboxScreen(
    threads: List<SmsThread>,
    loading: Boolean,
    onOpenConversation: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var selectedFilter by remember { mutableIntStateOf(0) }

    val filteredThreads = remember(threads, selectedFilter) {
        when (InboxFilter.entries[selectedFilter]) {
            InboxFilter.All -> threads
            InboxFilter.OTP -> threads.filter { t ->
                t.snippet.contains("code", true) || t.snippet.contains("OTP", true) ||
                    t.snippet.contains("verification", true) || t.snippet.contains("verify", true)
            }
            InboxFilter.Business -> threads.filter { t -> t.address.any { it.isLetter() } }
            InboxFilter.Personal -> threads.filter { t -> t.address.all { it.isDigit() || it == '+' || it == ' ' } }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 4.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 16.dp, end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "filter_chips") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    items(InboxFilter.entries.size) { index ->
                        val filter = InboxFilter.entries[index]
                        FilterChip(
                            selected = selectedFilter == index, onClick = { selectedFilter = index },
                            label = { Text(filter.label) }, shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
            if (loading) {
                item { Text("Loading messages…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (filteredThreads.isEmpty()) {
                item { Text("No messages", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(items = filteredThreads, key = { it.address }) { thread ->
                    SmsThreadCard(thread = thread, onClick = { onOpenConversation(thread.address) })
                }
            }
        }
    }
}

@Composable
private fun SmsThreadCard(thread: SmsThread, onClick: () -> Unit) {
    val initials = thread.address.take(2).uppercase()
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            SerafinaAvatar(imageUrl = null, initials = initials, hasUnread = thread.unreadCount > 0, size = 48.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = thread.address,
                    style = if (thread.unreadCount > 0) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    else MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(thread.snippet, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(thread.timestamp.toInboxTimestamp(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (thread.unreadCount > 0) {
                    Box(
                        modifier = Modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(thread.unreadCount.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// REAL SMS CONVERSATION
// ═══════════════════════════════════════════════════════════

@Composable
private fun RealConversationScreen(
    address: String,
    messages: List<SystemSms>,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SerafinaAvatar(imageUrl = null, initials = address.take(2).uppercase(), size = 36.dp)
                        Text(address, style = MaterialTheme.typography.titleMedium)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 12.dp, vertical = 8.dp).imePadding(),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (draft.isEmpty()) {
                        Text("Type a message…", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                    BasicTextField(
                        value = draft, onValueChange = { draft = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                IconButton(
                    onClick = { if (draft.isNotBlank()) { onSend(draft); draft = "" } },
                    enabled = draft.isNotBlank(),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = if (draft.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(items = messages, key = { it.id }) { sms ->
                MessageBubble(sms = sms)
            }
        }
    }
}

@Composable
private fun MessageBubble(sms: SystemSms) {
    val isOutbound = sms.isOutbound
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutbound) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (isOutbound) 16.dp else 4.dp,
                    bottomEnd = if (isOutbound) 4.dp else 16.dp,
                ))
                .background(if (isOutbound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Column {
                Text(
                    text = sms.body, style = MaterialTheme.typography.bodyMedium,
                    color = if (isOutbound) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = sms.timestamp.toInboxTimestamp(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOutbound) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// SETTINGS SCREEN — Real features wired from codebase
// ═══════════════════════════════════════════════════════════

@Composable
private fun SettingsScreen(
    themeViewModel: SerafinaThemeViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
    onRequestDefaultSms: () -> Unit,
) {
    val themeState by themeViewModel.state.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, contentDescription = "Close") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "general_header") { SettingsSectionHeader("General") }
            item(key = "general_card") {
                SettingsGroupCard {
                    SettingsRow(icon = Icons.Outlined.Sms, title = "Default SMS app", subtitle = "Tap to set Pulse as default", onClick = onRequestDefaultSms)
                    SettingsGroupDivider()
                    SettingsRow(icon = Icons.Outlined.Notifications, title = "Notifications", subtitle = "Coming soon")
                }
            }
            item(key = "appearance_header") { SettingsSectionHeader("Appearance") }
            item(key = "appearance_card") {
                SettingsGroupCard {
                    SettingsToggleRow(icon = Icons.Outlined.Palette, label = "Dynamic color", checked = themeState.dynamicColorEnabled, onCheckedChange = { themeViewModel.toggleDynamicColor() })
                    SettingsGroupDivider()
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text("Color palette", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(SerafinaPalette.entries.size) { index ->
                                val palette = SerafinaPalette.entries[index]
                                val isSelected = themeState.selectedPalette == palette
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(palette.seedColor)
                                        .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                                        .clickable { themeViewModel.selectPalette(palette) })
                                    Text(palette.label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    SettingsGroupDivider()
                    SettingsToggleRow(icon = Icons.Outlined.Accessibility, label = "Reduce motion", checked = themeState.reduceMotion, onCheckedChange = { themeViewModel.toggleReduceMotion() })
                }
            }
            item(key = "sync_header") { SettingsSectionHeader("Sync & Data") }
            item(key = "sync_card") {
                SettingsGroupCard {
                    SettingsRow(icon = Icons.Outlined.Cloud, title = "Sync environment", subtitle = settingsState.syncEnvironment.replaceFirstChar { it.uppercase() })
                    SettingsGroupDivider()
                    SettingsRow(icon = Icons.Outlined.Sync, title = "Background sync", subtitle = "WorkManager with exponential backoff")
                }
            }
            item(key = "privacy_header") { SettingsSectionHeader("Privacy & Security") }
            item(key = "privacy_card") {
                val protocolLabel = when (settingsState.protocolMode) {
                    ProtocolMode.PQXDH -> "Post-quantum (PQXDH)"
                    ProtocolMode.X3DH -> "Extended Triple DH (X3DH)"
                }
                val keyStorageLabel = when (settingsState.keyStoreCapability) {
                    is KeyStoreCapability.Available -> if (settingsState.isHardwareBacked) "Hardware-backed Keystore" else "Software Keystore"
                    KeyStoreCapability.SoftwareOnly -> "Software-only Keystore"
                    KeyStoreCapability.Unavailable -> "Unavailable"
                }
                val complianceLabel = if (settingsState.complianceLoaded) {
                    val s = settingsState.complianceStatus
                    when {
                        s.senderVerified && s.recipientVerified && s.identityVerified && s.tenDlcRegistered -> "Fully verified"
                        !s.tenDlcRegistered -> "10DLC registration pending"
                        !s.senderVerified -> "Sender verification pending"
                        !s.recipientVerified -> "Recipient verification pending"
                        !s.identityVerified -> "Identity verification pending"
                        else -> "Partially verified"
                    }
                } else "Loading…"
                SettingsGroupCard {
                    SettingsRow(icon = Icons.Outlined.Security, title = "Encryption protocol", subtitle = protocolLabel)
                    SettingsGroupDivider()
                    SettingsRow(icon = Icons.Outlined.Key, title = "Key storage", subtitle = keyStorageLabel)
                    SettingsGroupDivider()
                    SettingsRow(icon = Icons.Outlined.VerifiedUser, title = "Business verification", subtitle = complianceLabel)
                    SettingsGroupDivider()
                    SettingsRow(icon = Icons.Outlined.Lock, title = "End-to-end encryption", subtitle = "Messages encrypted before sync")
                }
            }
            item(key = "about_header") { SettingsSectionHeader("About") }
            item(key = "about_card") {
                SettingsGroupCard {
                    SettingsRow(icon = Icons.Outlined.Info, title = "Version", subtitle = settingsState.versionName)
                    SettingsGroupDivider()
                    SettingsRow(icon = Icons.Outlined.Cloud, title = "Environment", subtitle = settingsState.syncEnvironment)
                }
            }
            item(key = "bottom_spacer") { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Settings sub-components ──

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp))
}

@Composable
private fun SettingsGroupCard(content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) { Column { content() } }
}

@Composable
private fun SettingsGroupDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsToggleRow(icon: ImageVector, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// ═══════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════

private fun Instant.toInboxTimestamp(): String = when {
    atZone(ZoneId.systemDefault()).toLocalDate() == java.time.LocalDate.now() ->
        INBOX_TIME_FORMATTER.format(atZone(ZoneId.systemDefault()))
    else -> INBOX_DATE_FORMATTER.format(atZone(ZoneId.systemDefault()))
}

private val INBOX_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val INBOX_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
