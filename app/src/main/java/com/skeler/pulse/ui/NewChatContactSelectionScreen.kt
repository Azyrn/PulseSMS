@file:OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
)

package com.skeler.pulse.ui

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skeler.pulse.design.theme.SerafinaAppTheme
import com.skeler.pulse.design.theme.SerafinaPalette
import com.skeler.pulse.design.theme.SerafinaThemeMode
import com.skeler.pulse.design.theme.SerafinaThemeState
import com.skeler.pulse.design.util.rememberReducedMotionEnabled

@Immutable
internal data class ContactListItem(
    val key: String,
    val name: String,
    val phoneNumber: String,
)

@Immutable
internal data class ContactGroup(
    val label: String,
    val contacts: List<ContactListItem>,
)

@Immutable
private sealed interface ContactPickerRow {
    val key: String
    val contentType: String
}

@Immutable
private data class ContactPickerHeaderRow(
    override val key: String,
    val label: String,
) : ContactPickerRow {
    override val contentType: String = "new_chat_header"
}

@Immutable
private data class ContactPickerContactRow(
    override val key: String,
    val contact: ContactListItem,
) : ContactPickerRow {
    override val contentType: String = "new_chat_contact"
}

// ── Expressive design tokens ──

private object ContactPickerTokens {
    val avatarSize = 44.dp
    val rowHorizontalPadding = 20.dp
    val rowVerticalPadding = 10.dp
    val searchCardCorner = 28.dp
    val sectionHeaderPadding = 20.dp
}

private val AvatarPalette = listOf(
    Color(0xFF5AB7F2),
    Color(0xFF8E5AF7),
    Color(0xFF63D17D),
    Color(0xFFF05BC0),
    Color(0xFF79C7B3),
    Color(0xFFF2AE61),
)

@Composable
internal fun NewChatContactSelectionScreen(
    contactGroups: List<ContactGroup>,
    loading: Boolean,
    searchQuery: String,
    simOptions: List<NewChatSimOption>,
    selectedSimKey: String?,
    onContactClick: (ContactListItem) -> Unit,
    onBackClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    manualEntry: ContactListItem? = null,
    onManualEntryClick: ((ContactListItem) -> Unit)? = null,
    onSimOptionClick: ((NewChatSimOption) -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val reducedMotion = rememberReducedMotionEnabled()
    val listRows = remember(contactGroups) { contactGroups.flattenToPickerRows() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "New chat",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    scrolledContainerColor = colors.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            ExpressiveSearchCard(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                simOptions = simOptions,
                selectedSimKey = selectedSimKey,
                onSimOptionClick = onSimOptionClick,
                reducedMotion = reducedMotion,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                // ── Manual entry ──
                if (manualEntry != null && onManualEntryClick != null) {
                    item(key = "new_chat_manual_entry") {
                        NewChatManualEntryRow(
                            contact = manualEntry,
                            onClick = { onManualEntryClick(manualEntry) },
                        )
                    }
                }

                // ── Contact list ──
                if (loading) {
                    item(key = "new_chat_loading") {
                        NewChatLoadingState()
                    }
                } else if (listRows.isEmpty()) {
                    item(key = "new_chat_empty") {
                        NewChatEmptyState(query = searchQuery)
                    }
                } else {
                    items(
                        items = listRows,
                        key = { it.key },
                        contentType = { it.contentType },
                    ) { row ->
                        when (row) {
                            is ContactPickerHeaderRow -> NewChatSectionHeader(label = row.label)
                            is ContactPickerContactRow -> NewChatContactRow(
                                contact = row.contact,
                                onClick = { onContactClick(row.contact) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewChatLoadingState(
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Loading contacts…",
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ContactPickerTokens.rowHorizontalPadding, vertical = 24.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ── Expressive search card — elevated Surface with SIM chip ──

@Composable
private fun ExpressiveSearchCard(
    query: String,
    onQueryChange: (String) -> Unit,
    simOptions: List<NewChatSimOption>,
    selectedSimKey: String?,
    onSimOptionClick: ((NewChatSimOption) -> Unit)?,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val cardPadding = 18.dp
    val cardSpacing = 14.dp
    val searchFieldMinHeight = 36.dp

    val searchIconTint by animateColorAsState(
        targetValue = if (query.isBlank()) colors.onSurfaceVariant else colors.primary,
        animationSpec = tween(if (reducedMotion) 0 else 200),
        label = "search_icon_tint",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(ContactPickerTokens.searchCardCorner),
        color = colors.surfaceContainerLow,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(cardSpacing),
        ) {
            // ── Search field ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = searchIconTint,
                )
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = searchFieldMinHeight),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = colors.onSurface,
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Search,
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (query.isBlank()) {
                                Text(
                                    text = "Search name, number, or email",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }

            // ── SIM selector row ──
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SimCard,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colors.onSurfaceVariant,
                    )
                    Text(
                        text = "Send with",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    simOptions.forEach { option ->
                        val isSelected = option.key == selectedSimKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSimOptionClick?.invoke(option) },
                            label = {
                                Text(
                                    text = buildString {
                                        append(option.slotLabel)
                                        append(" · ")
                                        append(option.carrierLabel)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primaryContainer,
                                selectedLabelColor = colors.onPrimaryContainer,
                                containerColor = colors.surfaceContainerHigh,
                                labelColor = colors.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        }
    }
}

// ── Section header ──

@Composable
private fun NewChatSectionHeader(
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Text(
        text = label,
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(
                horizontal = ContactPickerTokens.sectionHeaderPadding,
                vertical = 8.dp,
            ),
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
        ),
        color = colors.primary,
    )
}

// ── Contact row — with phone number pills ──

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewChatContactRow(
    contact: ContactListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val avatarColor = remember(contact.name) {
        AvatarPalette[contact.name.trim().ifBlank { "#" }.hashCode().mod(AvatarPalette.size)]
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = ContactPickerTokens.rowHorizontalPadding,
                vertical = ContactPickerTokens.rowVerticalPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(ContactPickerTokens.avatarSize)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center,
        ) {
            val monogram = contact.name.contactInitial()
            if (monogram != null) {
                Text(
                    text = monogram,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!contact.name.isSameDisplayValueAs(contact.phoneNumber)) {
                PhoneNumberPill(number = contact.phoneNumber)
            }
        }
    }
}

// ── Phone number pill ──

@Composable
private fun PhoneNumberPill(
    number: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            text = number,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Manual entry row ──

@Composable
private fun NewChatManualEntryRow(
    contact: ContactListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = ContactPickerTokens.rowHorizontalPadding,
                vertical = ContactPickerTokens.rowVerticalPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(ContactPickerTokens.avatarSize)
                .clip(CircleShape)
                .background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = colors.onPrimary,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = contact.phoneNumber,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Start a new conversation",
                style = MaterialTheme.typography.bodySmall,
                color = colors.primary,
            )
        }
    }
}

// ── Empty state — expressive with icon ──

@Composable
private fun NewChatEmptyState(
    query: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ContactPickerTokens.rowHorizontalPadding, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.PersonSearch,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Text(
            text = if (query.isBlank()) "No contacts available" else "No matches for \"$query\"",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
        )
        Text(
            text = "Type a number manually to start a conversation.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}

// ── Helpers ──

internal fun List<NewChatRecipient>.toContactGroups(): List<ContactGroup> =
    groupBy { recipient ->
        recipient.sortLabel.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() }?.toString() ?: "#"
    }.toSortedMap().map { (label, recipients) ->
        ContactGroup(
            label = label,
            contacts = recipients.map { recipient ->
                ContactListItem(
                    key = recipient.key,
                    name = recipient.displayName,
                    phoneNumber = recipient.address,
                )
            },
        )
    }

internal fun mockContactGroups(): List<ContactGroup> {
    val contacts = listOf(
        ContactListItem(key = "aarif", name = "Aarif Khadija", phoneNumber = "+212603257455"),
        ContactListItem(key = "achraf", name = "Achraf Boksing", phoneNumber = "+212672206047"),
        ContactListItem(key = "adnan", name = "Adnan", phoneNumber = "+212673784670"),
        ContactListItem(key = "ayae", name = "Ayae", phoneNumber = "+212694215301"),
        ContactListItem(key = "baba", name = "Baba", phoneNumber = "+212612983744"),
        ContactListItem(key = "cheymae", name = "Cheymae", phoneNumber = "+212661420188"),
    )

    return contacts
        .groupBy { it.name.first().uppercaseChar().toString() }
        .toSortedMap()
        .map { (label, groupedContacts) ->
            ContactGroup(label = label, contacts = groupedContacts)
        }
    }

private fun List<ContactGroup>.flattenToPickerRows(): List<ContactPickerRow> = buildList {
    for (group in this@flattenToPickerRows) {
        add(
            ContactPickerHeaderRow(
                key = "new_chat_header_${group.label}",
                label = group.label,
            ),
        )
        group.contacts.forEach { contact ->
            add(
                ContactPickerContactRow(
                    key = "contact_${contact.key}",
                    contact = contact,
                ),
            )
        }
    }
}

private fun String.contactInitial(): String? =
    trim()
        .firstOrNull { it.isLetter() }
        ?.uppercase()

private fun String.isSameDisplayValueAs(other: String): Boolean {
    return normalizeDisplayValue() == other.normalizeDisplayValue()
}

private fun String.normalizeDisplayValue(): String {
    val trimmed = trim()
    if (trimmed.isBlank()) return ""
    if (trimmed.any { it.isLetter() } && !trimmed.contains('@')) {
        return trimmed.lowercase()
    }
    return trimmed.filter { it.isDigit() || it == '+' || it == '@' || it == '.' }.lowercase()
}

@Preview(
    name = "New Chat Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun NewChatContactSelectionScreenDarkPreview() {
    SerafinaAppTheme(
        darkTheme = true,
        themeState = SerafinaThemeState(
            dynamicColorEnabled = false,
            selectedPalette = SerafinaPalette.LavenderVolt,
            themeMode = SerafinaThemeMode.Dark,
        ),
    ) {
        NewChatContactSelectionScreen(
            contactGroups = mockContactGroups(),
            loading = false,
            searchQuery = "",
            simOptions = listOf(
                NewChatSimOption(
                    key = "sim_1",
                    subscriptionId = 1,
                    slotLabel = "SIM 1",
                    carrierLabel = "inwi",
                ),
                NewChatSimOption(
                    key = "sim_2",
                    subscriptionId = 2,
                    slotLabel = "SIM 2",
                    carrierLabel = "Orange",
                ),
            ),
            selectedSimKey = "sim_1",
            onContactClick = {},
            onBackClick = {},
            onSearchQueryChange = {},
        )
    }
}

@Preview(
    name = "New Chat Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun NewChatContactSelectionScreenLightPreview() {
    SerafinaAppTheme(
        darkTheme = false,
        themeState = SerafinaThemeState(
            dynamicColorEnabled = false,
            selectedPalette = SerafinaPalette.LavenderVolt,
            themeMode = SerafinaThemeMode.Light,
        ),
    ) {
        NewChatContactSelectionScreen(
            contactGroups = mockContactGroups(),
            loading = false,
            searchQuery = "",
            simOptions = listOf(
                NewChatSimOption(
                    key = "sim_1",
                    subscriptionId = 1,
                    slotLabel = "SIM 1",
                    carrierLabel = "inwi",
                ),
                NewChatSimOption(
                    key = "sim_2",
                    subscriptionId = 2,
                    slotLabel = "SIM 2",
                    carrierLabel = "Orange",
                ),
            ),
            selectedSimKey = "sim_1",
            onContactClick = {},
            onBackClick = {},
            onSearchQueryChange = {},
        )
    }
}
