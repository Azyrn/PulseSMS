package com.skeler.pulse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skeler.pulse.R
import com.skeler.pulse.contact.toBlockedSenderDisplayLabel
import com.skeler.pulse.design.util.elasticOverscroll
import com.skeler.pulse.design.util.motionAnimateItemModifier
import com.skeler.pulse.design.util.rememberEntranceModifier
import com.skeler.pulse.design.util.rememberMomentumFlingBehavior
import com.skeler.pulse.design.util.rememberReducedMotionEnabled
import java.security.MessageDigest

@Composable
internal fun BlockedNumbersScreen(
    blockedAddresses: Set<String>,
    listState: LazyListState,
    onBack: () -> Unit,
    onUnblock: (String) -> Unit,
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val listFlingBehavior = rememberMomentumFlingBehavior(enabled = !reducedMotion)
    val sortedAddresses = remember(blockedAddresses) {
        blockedAddresses.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
    }

    Scaffold(
        topBar = {
            SettingsTopBar(
                title = stringResource(R.string.blocked_numbers_title),
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            flingBehavior = listFlingBehavior,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .elasticOverscroll(
                    enabled = !reducedMotion,
                    state = listState,
                ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (sortedAddresses.isEmpty()) {
                item(key = "blocked_empty") {
                    InboxStateCard(
                        title = stringResource(R.string.blocked_empty_title),
                        body = stringResource(R.string.blocked_empty_body),
                        statusLabel = stringResource(R.string.blocked_empty_status),
                        actionLabel = stringResource(R.string.blocked_back_to_settings),
                        icon = Icons.Rounded.Block,
                        onAction = onBack,
                    )
                }
            } else {
                item(key = "blocked_header") {
                    SettingsSectionHeader(stringResource(R.string.blocked_count, sortedAddresses.size))
                }
                items(
                    items = sortedAddresses,
                    key = { address -> stableBlockedAddressListKey(address) },
                    contentType = { "blocked_address" },
                ) { address ->
                    val stableAddressKey = stableBlockedAddressListKey(address)
                    BlockedNumberRow(
                        address = address,
                        displayName = address.toBlockedSenderDisplayLabel(),
                        onUnblock = { onUnblock(address) },
                        modifier = motionAnimateItemModifier(reducedMotion)
                            .then(rememberEntranceModifier(stableAddressKey, reducedMotion)),
                    )
                }
            }
            item(key = "blocked_bottom_spacer") {
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun BlockedNumberRow(
    address: String,
    displayName: String,
    onUnblock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subtitle = address
        .removePrefix("phone:")
        .takeIf { address.startsWith("phone:") && it != displayName }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Block,
                    contentDescription = null,
                    modifier = Modifier.size(21.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            FilledTonalButton(
                onClick = onUnblock,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.blocked_unblock))
            }
        }
    }
}

private fun stableBlockedAddressListKey(address: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(address.toByteArray(Charsets.UTF_8))
    return "blocked_" + digest.take(12).joinToString(separator = "") { byte -> "%02x".format(byte) }
}
