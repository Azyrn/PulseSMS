package com.skeler.pulse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skeler.pulse.R
import com.skeler.pulse.design.component.SerafinaProgressIndicator
import com.skeler.pulse.design.component.StatusPill



@Composable
internal fun InboxLoadingStateCard(
    onRefreshInbox: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InboxStateCard(
        title = stringResource(R.string.inbox_loading_title),
        body = stringResource(R.string.inbox_loading_body),
        statusLabel = stringResource(R.string.inbox_loading_status),
        icon = Icons.Rounded.HourglassTop,
        actionLabel = stringResource(R.string.inbox_loading_action),
        onAction = onRefreshInbox,
        modifier = modifier,
    ) {
        SerafinaProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun InboxEmptyStateCard(
    onOpenNewChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InboxStateCard(
        title = stringResource(R.string.inbox_empty_title),
        body = stringResource(R.string.inbox_empty_body),
        statusLabel = stringResource(R.string.inbox_empty_status),
        icon = Icons.Rounded.AddComment,
        actionLabel = stringResource(R.string.inbox_empty_action),
        onAction = onOpenNewChat,
        modifier = modifier,
    )
}

@Composable
internal fun InboxFilteredEmptyStateCard(
    activeFilter: String,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InboxStateCard(
        title = stringResource(R.string.inbox_nothing_in_filter, activeFilter),
        body = stringResource(R.string.inbox_filter_empty_body),
        statusLabel = stringResource(R.string.inbox_filter_status, activeFilter),
        icon = Icons.Rounded.Search,
        actionLabel = stringResource(R.string.inbox_show_all),
        onAction = onShowAll,
        modifier = modifier,
    )
}

@Composable
internal fun InboxErrorStateCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InboxStateCard(
        title = stringResource(R.string.inbox_unavailable),
        body = message,
        statusLabel = stringResource(R.string.inbox_read_problem),
        icon = Icons.Rounded.ErrorOutline,
        actionLabel = stringResource(R.string.inbox_try_again),
        onAction = onRetry,
        modifier = modifier,
    )
}

@Composable
internal fun InboxStateCard(
    title: String,
    body: String,
    statusLabel: String,
    icon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    supportingContent: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                    ),
                ),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                        tonalElevation = 0.dp,
                        modifier = Modifier.size(52.dp),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = accentColor,
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        StatusPill(
                            label = statusLabel,
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                supportingContent?.invoke()
                FilledTonalButton(onClick = onAction) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(actionLabel)
                }
            }
        }
    }
}
