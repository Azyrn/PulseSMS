package com.skeler.pulse.sms

internal data class ParseResult(
    val emoji: String,
    val referencedText: String,
)

internal object ReactionParser {

    private val actionToEmoji = mapOf(
        "Loved" to "❤️",
        "Liked" to "👍",
        "Emphasized" to "😄",
        "Questioned" to "😮",
        "Laughed at" to "😂",
    )

    val emojiToAction: Map<String, String> =
        actionToEmoji.entries.associate { (k, v) -> v to k }

    private val allActions = actionToEmoji.keys.joinToString("|")
    private val iosPattern = Regex("""($allActions)\s+"(.+)"""")

    // Pulse-to-Pulse: arbitrary emoji "quoted text"
    private val rawEmojiPattern = Regex("""([\p{So}\p{Sk}]+)\s+"(.+)"""")

    // iOS MMS reactions: Liked an image / Loved a video / Laughed at a sticker
    private val mmsReactionPattern = Regex("""($allActions)\s+(an image|a video|a sticker)""")
    private val rawMmsPattern = Regex("""([\p{So}\p{Sk}]+)\s+(an image|a video|a sticker)""")

    fun parseReaction(body: String): ParseResult? {
        iosPattern.matchEntire(body.trim())?.let { match ->
            val action = match.groupValues[1]
            val text = normalizeText(match.groupValues[2])
            return actionToEmoji[action]?.let { emoji ->
                ParseResult(emoji = emoji, referencedText = text)
            }
        }

        rawEmojiPattern.matchEntire(body.trim())?.let { match ->
            val emoji = match.groupValues[1]
            val text = normalizeText(match.groupValues[2])
            return ParseResult(emoji = emoji, referencedText = text)
        }

        mmsReactionPattern.matchEntire(body.trim())?.let { match ->
            val action = match.groupValues[1]
            return actionToEmoji[action]?.let { emoji ->
                ParseResult(emoji = emoji, referencedText = "")
            }
        }

        rawMmsPattern.matchEntire(body.trim())?.let { match ->
            val emoji = match.groupValues[1]
            return ParseResult(emoji = emoji, referencedText = "")
        }

        return null
    }

    fun isReactionMessage(body: String): Boolean =
        iosPattern.matches(body.trim()) ||
        rawEmojiPattern.matches(body.trim()) ||
        mmsReactionPattern.matches(body.trim()) ||
        rawMmsPattern.matches(body.trim())

    fun encodeReactionSms(emoji: String, referencedText: String): String {
        val action = emojiToAction[emoji]
        val text = referencedText.ifBlank { "a message" }
        return if (action != null) {
            "$action \"$text\""
        } else {
            "$emoji \"$text\""
        }
    }

    fun findAllMatchingMessages(
        referencedText: String,
        messages: List<SystemSms>,
    ): List<SystemSms> {
        if (referencedText.isBlank()) return emptyList()

        val normalized = normalizeText(referencedText)

        return messages.filter { normalizeText(it.body) == normalized }
    }

    fun findMessageMatch(
        referencedText: String,
        messages: List<SystemSms>,
    ): SystemSms? {
        if (referencedText.isBlank()) return null

        val normalized = normalizeText(referencedText)

        return messages.lastOrNull { normalizeText(it.body) == normalized }
    }

    private fun normalizeText(text: String): String =
        text.trim().replace(Regex("\\s+"), " ")
}
