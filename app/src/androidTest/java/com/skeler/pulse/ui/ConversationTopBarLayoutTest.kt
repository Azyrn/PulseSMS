package com.skeler.pulse.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.skeler.pulse.design.theme.SerafinaAppTheme
import com.skeler.pulse.design.theme.SerafinaThemeState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationTopBarLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun should_pin_call_action_to_trailing_edge_when_address_is_dialable() {
        composeRule.setContent {
            SerafinaAppTheme(themeState = SerafinaThemeState(dynamicColorEnabled = false)) {
                val avatarColors = MaterialTheme.colorScheme.conversationAvatarColors(ConversationTitle)

                Box(modifier = Modifier.fillMaxWidth()) {
                    ConversationTopBar(
                        title = ConversationTitle,
                        address = "+1 (555) 123-4567",
                        messages = emptyList(),
                        unreadCount = 0,
                        importantCount = 0,
                        avatarColors = avatarColors,
                        onBack = {},
                        onCallAddress = {},
                    )
                }
            }
        }

        val rootBounds = composeRule.onRoot().getUnclippedBoundsInRoot()
        val callBounds = composeRule
            .onNodeWithContentDescription("Call $ConversationTitle")
            .getUnclippedBoundsInRoot()

        assertTrue(callBounds.right >= rootBounds.right - MaximumTrailingCallActionInset)
    }

    private companion object {
        const val ConversationTitle = "Ada"
        val MaximumTrailingCallActionInset = 72.dp
    }
}
