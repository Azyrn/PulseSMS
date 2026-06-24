package com.skeler.pulse.ui
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MarkunreadMailbox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skeler.pulse.InboxAccessState
import com.skeler.pulse.R
import com.skeler.pulse.design.component.StatusPill


@Composable
internal fun InboxOnboardingScreen(
    accessState: InboxAccessState,
    hasPendingLaunchRequest: Boolean,
    onRequestSmsPermissions: () -> Unit,
    onRequestDefaultSms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAlreadyDefault = !accessState.permissionDenied && accessState.isDefaultSmsApp

    val title: String
    val body: String
    val ctaLabel: String
    val ctaIcon: ImageVector
    val onCtaClick: () -> Unit
    val statusLabel: String

    if (accessState.permissionDenied) {
        title = stringResource(R.string.inbox_unlock_title)
        body = stringResource(R.string.inbox_unlock_body)
        ctaLabel = stringResource(R.string.inbox_unlock_cta)
        ctaIcon = Icons.Rounded.Key
        onCtaClick = onRequestSmsPermissions
        statusLabel = stringResource(R.string.inbox_unlock_status)
    } else if (isAlreadyDefault) {
        title = stringResource(R.string.inbox_default_active_title)
        body = stringResource(R.string.inbox_default_active_body)
        ctaLabel = stringResource(R.string.inbox_default_active_cta)
        ctaIcon = Icons.Rounded.CheckCircle
        onCtaClick = onRequestDefaultSms
        statusLabel = stringResource(R.string.inbox_default_active_status)
    } else {
        title = stringResource(R.string.inbox_default_required_title)
        body = stringResource(R.string.inbox_default_required_body)
        ctaLabel = stringResource(R.string.inbox_default_required_cta)
        ctaIcon = Icons.Rounded.MarkunreadMailbox
        onCtaClick = onRequestDefaultSms
        statusLabel = stringResource(R.string.inbox_default_required_status)
    }

    val colors = MaterialTheme.colorScheme

    Scaffold(
        modifier = modifier,
        containerColor = colors.surface,
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = colors.screenBackgroundBrush {
                        Brush.verticalGradient(
                            colors = listOf(
                                colors.surface,
                                colors.primary.copy(alpha = 0.08f),
                                colors.tertiary.copy(alpha = 0.12f),
                            ),
                        )
                    },
                ),
        ) {
            val cardWidth = if (maxWidth > 720.dp) 520.dp else maxWidth - 32.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier.widthIn(max = cardWidth),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 0.dp,
                    border = BorderStroke(
                        width = 1.dp,
                        brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)),
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        StatusPill(
                            label = statusLabel,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = 0.dp,
                            modifier = Modifier.size(72.dp),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = ctaIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Text(
                                text = body,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (hasPendingLaunchRequest) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.68f),
                                tonalElevation = 0.dp,
                            ) {
                                Text(
                                    text = stringResource(R.string.inbox_pending_launch_body),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                        Button(
                            onClick = onCtaClick,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = ctaIcon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(ctaLabel)
                        }
                    }
                }
            }
        }
    }
}
