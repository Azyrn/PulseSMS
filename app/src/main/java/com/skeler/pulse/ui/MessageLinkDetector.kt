package com.skeler.pulse.ui

internal data class MessageLinkTarget(
    val text: String,
    val uri: String,
    val start: Int,
    val end: Int,
)

internal object MessageLinkDetector {
    private val urlPattern = Regex("\\b((?:https?://|www\\.)[^\\s<>()]+)", RegexOption.IGNORE_CASE)
    private val emailPattern = Regex("\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b", RegexOption.IGNORE_CASE)
    private val phonePattern = Regex("(?<![\\w@])(?:\\+?\\d[\\d .()/-]{5,}\\d)(?![\\w@])")
    private val trailingPunctuation = setOf('.', ',', ';', ':', '!', '?', ')', ']')

    fun detectTargets(messageBody: String): List<MessageLinkTarget> {
        if (messageBody.isBlank()) return emptyList()

        val targets = mutableListOf<MessageLinkTarget>()
        collectTargets(messageBody, urlPattern, targets, ::webUriFor)
        collectTargets(messageBody, emailPattern, targets, ::emailUriFor)
        collectTargets(messageBody, phonePattern, targets, ::phoneUriFor)
        return targets.sortedBy { target -> target.start }
    }

    private fun collectTargets(
        messageBody: String,
        pattern: Regex,
        targets: MutableList<MessageLinkTarget>,
        uriFor: (String) -> String?,
    ) {
        pattern.findAll(messageBody).forEach { match ->
            val normalized = messageBody.normalizedTarget(match.range.first, match.range.last + 1)
            if (targets.any { target -> target.overlaps(normalized.first, normalized.second) }) return@forEach

            uriFor(normalized.third)?.let { uri ->
                targets += MessageLinkTarget(
                    text = normalized.third,
                    uri = uri,
                    start = normalized.first,
                    end = normalized.second,
                )
            }
        }
    }

    private fun String.normalizedTarget(start: Int, end: Int): Triple<Int, Int, String> {
        var normalizedEnd = end
        while (normalizedEnd > start && get(normalizedEnd - 1) in trailingPunctuation) {
            normalizedEnd -= 1
        }
        return Triple(start, normalizedEnd, substring(start, normalizedEnd))
    }

    private fun MessageLinkTarget.overlaps(start: Int, end: Int): Boolean =
        this.start < end && start < this.end

    private fun webUriFor(text: String): String =
        if (text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)) {
            text
        } else {
            "https://$text"
        }

    private fun emailUriFor(text: String): String = "mailto:$text"

    private fun phoneUriFor(text: String): String? {
        val normalized = buildString {
            text.forEachIndexed { index, character ->
                when {
                    character.isDigit() -> append(character)
                    character == '+' && index == 0 -> append(character)
                }
            }
        }
        val digitCount = normalized.count(Char::isDigit)
        if (digitCount < MIN_PHONE_DIGITS) return null
        return "tel:$normalized"
    }

    private const val MIN_PHONE_DIGITS = 7
}
