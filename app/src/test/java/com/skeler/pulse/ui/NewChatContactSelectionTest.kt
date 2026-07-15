package com.skeler.pulse.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.skeler.pulse.design.theme.SerafinaDarkColorScheme
import com.skeler.pulse.design.theme.SerafinaLightColorScheme
import com.skeler.pulse.sms.SmsThread
import org.junit.Assert.assertEquals
import org.junit.Test

private const val WCAG_MINIMUM_NORMAL_TEXT_CONTRAST_RATIO = 4.5f

class NewChatContactSelectionTest {
    @Test
    fun should_group_contacts_by_uppercase_sort_label_when_recipients_are_loaded() {
        val groups = listOf(
            NewChatRecipient(
                key = "achraf",
                displayName = "Achraf Boksing",
                address = "07 06 22 75 62",
                sortLabel = "Achraf Boksing",
            ),
            NewChatRecipient(
                key = "baba",
                displayName = "Baba",
                address = "06 48 87 12 84",
                sortLabel = "Baba",
            ),
            NewChatRecipient(
                key = "aarif",
                displayName = "Aarif Khadija",
                address = "06 57 24 06 32",
                sortLabel = "Aarif Khadija",
            ),
        ).toContactGroups()

        assertEquals(listOf("A", "B"), groups.map { group -> group.label })
        assertEquals(listOf("Achraf Boksing", "Aarif Khadija"), groups.first().contacts.map { contact -> contact.name })
    }

    @Test
    fun should_keep_new_chat_recipients_limited_to_saved_contacts_when_threads_exist() {
        val savedContacts = listOf(
            NewChatRecipient(
                key = "contact_saved",
                displayName = "Achraf Boksing",
                address = "+212672206047",
                sortLabel = "Achraf Boksing",
            ),
        )
        val recentThreads = listOf(
            smsThread(address = "+212672206047"),
            smsThread(address = "+212600000000"),
        )

        val recipients = savedContacts.mergeThreadRecipients(recentThreads)

        assertEquals(listOf("Achraf Boksing"), recipients.map { recipient -> recipient.displayName })
    }

    @Test
    fun should_create_consistent_contact_avatar_initials_from_display_name() {
        assertEquals("AB", "Achraf Boksing".contactAvatarInitials())
        assertEquals("A", "achraf".contactAvatarInitials())
        assertEquals(null, "+212 672 206 047".contactAvatarInitials())
    }

    @Test
    fun should_render_compact_sim_slot_number_when_slot_label_contains_digits() {
        val option = NewChatSimOption(
            key = "sim_1",
            subscriptionId = 5,
            slotLabel = "SIM 1",
            carrierLabel = "inwi",
        )

        assertEquals("1", option.slotIndicator())
    }

    @Test
    fun should_fallback_to_subscription_id_when_slot_label_has_no_digits() {
        val option = NewChatSimOption(
            key = "sim_default",
            subscriptionId = 2,
            slotLabel = "Default line",
            carrierLabel = "inwi",
        )

        assertEquals("2", option.slotIndicator())
    }

    @Test
    fun should_toggle_to_next_sim_when_selected_sim_is_available() {
        val options = listOf(
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
        )

        assertEquals("sim_2", options.nextSimOption("sim_1")?.key)
        assertEquals("sim_1", options.nextSimOption("sim_2")?.key)
    }

    @Test
    fun should_keep_only_sim_when_toggle_has_single_option() {
        val options = listOf(
            NewChatSimOption(
                key = "sim_1",
                subscriptionId = 1,
                slotLabel = "SIM 1",
                carrierLabel = "inwi",
            ),
        )

        assertEquals("sim_1", options.nextSimOption("sim_1")?.key)
    }

    @Test
    fun should_render_clear_composer_sim_badge() {
        val option = NewChatSimOption(
            key = "sim_2",
            subscriptionId = 22,
            slotLabel = "SIM 2",
            carrierLabel = "Orange",
        )

        assertEquals("SIM 2", option.composerSimBadgeLabel())
    }

    @Test
    fun should_offset_conversation_timeline_scroll_target_after_header() {
        assertEquals(1, conversationTimelineLazyListIndex(timelineIndex = 0))
        assertEquals(5, conversationTimelineLazyListIndex(timelineIndex = 4))
    }

    @Test
    fun should_create_mock_contact_groups_with_shared_grouping_rules() {
        val groups = mockContactGroups()

        assertEquals(listOf("A", "B", "C"), groups.map { group -> group.label })
        assertEquals(listOf("Aarif Khadija", "Achraf Boksing", "Adnan", "Ayae"), groups.first().contacts.map { contact -> contact.name })
    }

    @Test
    fun should_keep_static_avatar_palette_above_wcag_contrast_threshold() {
        val lowContrastRatios = listOf(
            SerafinaLightColorScheme,
            SerafinaDarkColorScheme,
        )
            .flatMap { colorScheme -> colorScheme.contactAvatarColorPalette() }
            .map { avatarColors -> avatarColors.contentColor.contrastRatioAgainst(avatarColors.containerColor) }
            .filter { contrastRatio -> contrastRatio < WCAG_MINIMUM_NORMAL_TEXT_CONTRAST_RATIO }

        assertEquals(emptyList<Float>(), lowContrastRatios)
    }
}

private fun Color.contrastRatioAgainst(background: Color): Float {
    val foregroundLuminance = luminance() + 0.05f
    val backgroundLuminance = background.luminance() + 0.05f
    return maxOf(foregroundLuminance, backgroundLuminance) / minOf(foregroundLuminance, backgroundLuminance)
}

private fun smsThread(address: String): SmsThread = SmsThread(
    threadId = 1L,
    address = address,
    snippet = "hello",
    date = 1L,
    messageCount = 1,
    unreadCount = 0,
)
