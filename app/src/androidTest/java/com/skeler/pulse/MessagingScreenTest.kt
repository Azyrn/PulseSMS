package com.skeler.pulse

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.skeler.pulse.contracts.messaging.BusinessPriority
import com.skeler.pulse.contracts.messaging.ComposerState
import com.skeler.pulse.contracts.messaging.ConversationSyncState
import com.skeler.pulse.contracts.messaging.MessageDraft
import com.skeler.pulse.contracts.messaging.MessageTimeline
import com.skeler.pulse.contracts.messaging.MessagingState
import com.skeler.pulse.contracts.messaging.SendBlockReason
import com.skeler.pulse.contracts.messaging.SendEligibility
import com.skeler.pulse.design.theme.PulseTheme
import com.skeler.pulse.messaging.ui.MessagingScreen
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class MessagingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun renders_empty_state() {
        composeRule.setContent {
            PulseTheme {
                MessagingScreen(
                    state = baseState(),
                    onIntent = {},
                )
            }
        }

        composeRule.onNodeWithText("No messages yet").assertIsDisplayed()
        composeRule.onNodeWithText("Start the conversation with a new message below.").assertIsDisplayed()
    }

    @Test
    fun renders_sync_failure_banner() {
        composeRule.setContent {
            PulseTheme {
                MessagingScreen(
                    state = baseState(
                        sync = ConversationSyncState.Failed(
                            com.skeler.pulse.contracts.errors.NetworkError.Unreachable(
                                message = "Sync gateway is currently unreachable",
                            )
                        ),
                    ),
                    onIntent = {},
                )
            }
        }

        composeRule.onNodeWithText("Sync failed").assertIsDisplayed()
        composeRule.onNodeWithText("Sync gateway is currently unreachable").assertIsDisplayed()
    }

    @Test
    fun blocked_composer_disables_send() {
        composeRule.setContent {
            PulseTheme {
                MessagingScreen(
                    state = baseState(
                        composer = ComposerState(
                            draft = MessageDraft(
                                text = "Outbound business copy",
                                attachments = persistentListOf(),
                                priority = BusinessPriority.Normal,
                            ),
                            eligibility = SendEligibility.Blocked(
                                SendBlockReason.TenDlcRegistrationPending,
                            ),
                        ),
                    ),
                    onIntent = {},
                )
            }
        }

        composeRule.onNodeWithText("Registration pending").assertIsDisplayed()
        composeRule.onNodeWithText("Send").assertIsNotEnabled()
    }

    private fun baseState(
        sync: ConversationSyncState = ConversationSyncState.UpToDate,
        composer: ComposerState = ComposerState(
            draft = MessageDraft(
                text = "",
                attachments = persistentListOf(),
                priority = BusinessPriority.Normal,
            ),
            eligibility = SendEligibility.Allowed,
        ),
    ): MessagingState = MessagingState.Ready(
        conversationId = "business-primary",
        timeline = MessageTimeline(items = persistentListOf()),
        composer = composer,
        sync = sync,
        lastSyncedAt = Instant.parse("2026-04-17T09:00:00Z"),
        complianceUpdatedAt = null,
        surfaceError = null,
    )
}
