package com.skeler.pulse.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageLinkDetectorTest {
    @Test
    fun should_detect_https_url_and_strip_trailing_sentence_punctuation() {
        val targets = MessageLinkDetector.detectTargets("Track it at https://pulse.example/status.")

        assertEquals(
            listOf(
                MessageLinkTarget(
                    text = "https://pulse.example/status",
                    uri = "https://pulse.example/status",
                    start = 12,
                    end = 40,
                ),
            ),
            targets,
        )
    }

    @Test
    fun should_detect_www_email_and_phone_targets() {
        val targets = MessageLinkDetector.detectTargets("Use www.pulse.example or help@pulse.example or +1 (415) 555-0184")

        assertEquals(
            listOf(
                MessageLinkTarget(
                    text = "www.pulse.example",
                    uri = "https://www.pulse.example",
                    start = 4,
                    end = 21,
                ),
                MessageLinkTarget(
                    text = "help@pulse.example",
                    uri = "mailto:help@pulse.example",
                    start = 25,
                    end = 43,
                ),
                MessageLinkTarget(
                    text = "+1 (415) 555-0184",
                    uri = "tel:+14155550184",
                    start = 47,
                    end = 64,
                ),
            ),
            targets,
        )
    }

    @Test
    fun should_not_treat_short_verification_code_as_phone_number() {
        val targets = MessageLinkDetector.detectTargets("Your verification code is 493827")

        assertEquals(emptyList<MessageLinkTarget>(), targets)
    }
}
