package com.example.util

/**
 * Custom End-to-End Encryption (E2EE) Substitution Cipher for Linko.
 *
 * Provides two primary machines:
 * 1. [encodeMessage]: Converts plain text characters into Linko's custom symbol dictionary,
 *    joining each encoded token with a single space (" ") while preserving spaces (" " -> "___"),
 *    case (uppercase and lowercase), numbers, punctuation, and emojis.
 *    CRITICAL URL BYPASS: Any URLs (e.g. YouTube links, https://, http://, www.) are detected
 *    via [URL_REGEX] and left completely unencoded as raw plain text so cipher slashes
 *    never conflict with URLs.
 * 2. [decodeMessage]: Safely checks whether a string is encoded in [SecretMessageCoder] format
 *    before splitting and decoding. Detects raw URL tokens and keeps them completely intact
 *    without mangling their slashes or query parameters.
 */
object SecretMessageCoder {

    /**
     * Regex for detecting standard web URLs (http, https, www).
     */
    val URL_REGEX = Regex(
        """(https?://[^\s]+|www\.[^\s]+)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Exact character-to-symbol mapping dictionary (Uppercase A-Z, Lowercase a-z, Space, Newline).
     */
    private val charToSymbolMap: Map<Char, String> = mapOf(
        'A' to "/.",
        'B' to "//",
        'C' to ".#",
        'D' to "##",
        'E' to "..",
        'F' to "><",
        'G' to ">>",
        'H' to "<<",
        'I' to "||",
        'J' to ".-.",
        'K' to "..-",
        'L' to "--.",
        'M' to "---",
        'N' to "-.",
        'O' to "::",
        'P' to "[]",
        'Q' to "{}",
        'R' to "~~",
        'S' to "*/",
        'T' to "++",
        'U' to "()",
        'V' to "\\/",
        'W' to "\\/\\/",
        'X' to "><>",
        'Y' to "->",
        'Z' to "<-",
        'a' to "^/.",
        'b' to "^//",
        'c' to "^.#",
        'd' to "^##",
        'e' to "^..",
        'f' to "^><",
        'g' to "^>>",
        'h' to "^<<",
        'i' to "^||",
        'j' to "^.-.",
        'k' to "^..-",
        'l' to "^--.",
        'm' to "^---",
        'n' to "^-.",
        'o' to "^::",
        'p' to "^[]",
        'q' to "^{}",
        'r' to "^~~",
        's' to "^*/",
        't' to "^++",
        'u' to "^()",
        'v' to "^\\/",
        'w' to "^\\/\\/",
        'x' to "^><>",
        'y' to "^->",
        'z' to "^<-",
        ' ' to "___",
        '\n' to "_NL_",
        '\t' to "_TB_"
    )

    /**
     * Reverse symbol-to-character lookup dictionary used by the Decoder Machine.
     */
    private val symbolToCharMap: Map<String, Char> =
        charToSymbolMap.entries.associate { (char, symbol) -> symbol to char }

    /**
     * Helper to separate trailing punctuation (e.g. sentence-ending periods, commas, or parentheses)
     * from a detected URL so the punctuation is encoded as text and not baked into the link.
     */
    fun splitUrlAndTrailingPunctuation(rawUrl: String): Pair<String, String> {
        var url = rawUrl
        var trailing = ""
        val punctuation = ".,!?:;\"'()[]<>"
        while (url.isNotEmpty() && punctuation.contains(url.last())) {
            // Keep closing parenthesis/bracket if the URL itself has a matching open one
            if (url.endsWith(")") && url.count { it == '(' } >= url.count { it == ')' }) {
                break
            }
            if (url.endsWith("]") && url.count { it == '[' } >= url.count { it == ']' }) {
                break
            }
            trailing = url.last() + trailing
            url = url.dropLast(1)
        }
        return Pair(url, trailing)
    }

    /**
     * Checks if a token is a URL (starts with http://, https://, www., or matches URL_REGEX).
     */
    fun isUrlToken(token: String): Boolean {
        if (token.isBlank()) return false
        return token.startsWith("http://", ignoreCase = true) ||
            token.startsWith("https://", ignoreCase = true) ||
            token.startsWith("www.", ignoreCase = true) ||
            URL_REGEX.matches(token)
    }

    /**
     * Encoder Machine:
     * Converts characters in [plainText] into custom cipher symbols, joining tokens with a single space.
     *
     * URL DETECTION & BYPASS:
     * Any URLs matching [URL_REGEX] are left completely unencoded as raw plain text tokens,
     * while the regular text before, between, and after URLs is encoded.
     */
    fun encodeMessage(plainText: String): String {
        if (plainText.isEmpty()) return ""
        if (isEncodedFormat(plainText)) return plainText

        val tokens = mutableListOf<String>()
        var lastIndex = 0

        val matches = URL_REGEX.findAll(plainText).toList()
        for (match in matches) {
            val range = match.range
            val rawMatch = match.value

            // Encode regular text before this URL
            if (range.first > lastIndex) {
                val textSegment = plainText.substring(lastIndex, range.first)
                tokens.addAll(encodePlainTextSegment(textSegment))
            }

            // Separate clean URL from trailing sentence punctuation
            val (cleanUrl, trailingPunctuation) = splitUrlAndTrailingPunctuation(rawMatch)

            // Add URL as raw plain text token (completely unencoded)
            if (cleanUrl.isNotEmpty()) {
                tokens.add(cleanUrl)
            }

            // If there was trailing punctuation, encode it as text
            if (trailingPunctuation.isNotEmpty()) {
                tokens.addAll(encodePlainTextSegment(trailingPunctuation))
            }

            lastIndex = range.last + 1
        }

        // Encode remaining text after the last URL
        if (lastIndex < plainText.length) {
            val textSegment = plainText.substring(lastIndex)
            tokens.addAll(encodePlainTextSegment(textSegment))
        }

        return tokens.joinToString(" ")
    }

    /**
     * Encodes a regular text segment character-by-character.
     */
    private fun encodePlainTextSegment(segment: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < segment.length) {
            val ch = segment[i]
            // Preserve multi-char UTF-16 surrogate pairs (e.g., emojis)
            if (ch.isHighSurrogate() && i + 1 < segment.length && segment[i + 1].isLowSurrogate()) {
                tokens.add(segment.substring(i, i + 2))
                i += 2
            } else {
                tokens.add(charToSymbolMap[ch] ?: ch.toString())
                i += 1
            }
        }
        return tokens
    }

    /**
     * Decoder Machine:
     * Safely decodes a [SecretMessageCoder] payload back into readable plain text.
     *
     * SAFE URL DECODING:
     * Safely reads strings containing a mix of plain text URLs and encoded cipher symbols.
     * URL tokens are preserved verbatim without looking up cipher mappings (preventing
     * slashes in URLs from colliding with '/.' or '//' cipher mappings).
     */
    fun decodeMessage(encodedText: String): String {
        if (encodedText.isEmpty()) return ""
        if (!isEncodedFormat(encodedText)) {
            return encodedText
        }

        return encodedText
            .split(" ")
            .filter { it.isNotEmpty() }
            .joinToString("") { token ->
                if (isUrlToken(token)) {
                    token
                } else {
                    symbolToCharMap[token]?.toString() ?: token
                }
            }
    }

    /**
     * Helper to check whether a raw string is genuinely encoded with [SecretMessageCoder].
     * Supports mixed messages containing both cipher symbols and raw URLs.
     */
    fun isEncodedFormat(text: String): Boolean {
        if (text.isBlank()) return false
        if (text.startsWith("ENC:v1:")) return false

        val tokens = text.split(" ").filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return false

        return tokens.all { token ->
            symbolToCharMap.containsKey(token) ||
                isUrlToken(token) ||
                token.length == 1 ||
                (token.length == 2 && token[0].isHighSurrogate() && token[1].isLowSurrogate())
        } && tokens.any { symbolToCharMap.containsKey(it) }
    }
}
