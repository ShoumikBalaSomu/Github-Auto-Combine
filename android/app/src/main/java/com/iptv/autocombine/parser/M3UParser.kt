package com.iptv.autocombine.parser

import com.iptv.autocombine.model.Channel

/**
 * Parser for M3U/M3U8 playlist files.
 *
 * Supports the extended M3U format with EXTINF directives including:
 * - tvg-id: EPG identifier
 * - tvg-name: EPG display name
 * - tvg-logo: Channel logo URL
 * - group-title: Channel group/category
 * - tvg-language: Channel language
 * - tvg-country: Country code
 * - Additional custom attributes
 *
 * Example M3U entry:
 * ```
 * #EXTINF:-1 tvg-id="BBC1.uk" tvg-name="BBC One" tvg-logo="http://logo.url" group-title="UK",BBC One HD
 * http://stream.url/live/bbc1.m3u8
 * ```
 */
object M3UParser {

    // Regex to extract key="value" pairs from EXTINF line
    private val ATTRIBUTE_REGEX = Regex("""([\w-]+)="([^"]*?)"""")

    // Known attribute keys mapped to Channel fields
    private const val KEY_TVG_ID = "tvg-id"
    private const val KEY_TVG_NAME = "tvg-name"
    private const val KEY_TVG_LOGO = "tvg-logo"
    private const val KEY_GROUP_TITLE = "group-title"
    private const val KEY_TVG_LANGUAGE = "tvg-language"
    private const val KEY_TVG_COUNTRY = "tvg-country"

    /**
     * Parses raw M3U content into a list of [Channel] objects.
     *
     * @param content Raw M3U/M3U8 file content as a string
     * @return List of parsed channels, empty list if content is invalid
     */
    fun parse(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines().map { it.trim() }

        var i = 0
        while (i < lines.size) {
            val line = lines[i]

            if (line.startsWith("#EXTINF:")) {
                // Parse the EXTINF line
                val extInf = line

                // Extract all key="value" attributes
                val attributes = mutableMapOf<String, String>()
                for (match in ATTRIBUTE_REGEX.findAll(extInf)) {
                    val key = match.groupValues[1].lowercase()
                    val value = match.groupValues[2]
                    attributes[key] = value
                }

                // Extract the display name (text after the last comma in EXTINF line)
                val displayName = extractDisplayName(extInf)

                // Find the URL on the next non-empty, non-comment line
                var url = ""
                var j = i + 1
                while (j < lines.size) {
                    val nextLine = lines[j]
                    if (nextLine.isNotBlank() && !nextLine.startsWith("#")) {
                        url = nextLine
                        break
                    } else if (nextLine.startsWith("#EXTINF:")) {
                        // Next EXTINF without a URL, skip
                        break
                    }
                    j++
                }

                if (url.isNotBlank()) {
                    // Build known attribute fields
                    val tvgId = attributes.remove(KEY_TVG_ID) ?: ""
                    val tvgName = attributes.remove(KEY_TVG_NAME) ?: ""
                    val tvgLogo = attributes.remove(KEY_TVG_LOGO) ?: ""
                    val groupTitle = attributes.remove(KEY_GROUP_TITLE) ?: ""
                    val language = attributes.remove(KEY_TVG_LANGUAGE) ?: ""
                    val country = attributes.remove(KEY_TVG_COUNTRY) ?: ""

                    channels.add(
                        Channel(
                            name = displayName,
                            url = url,
                            logoUrl = tvgLogo,
                            group = groupTitle,
                            tvgId = tvgId,
                            tvgName = tvgName,
                            tvgLogo = tvgLogo,
                            language = language,
                            country = country,
                            extras = attributes.toMap()
                        )
                    )

                    i = j + 1
                    continue
                }
            }

            i++
        }

        return channels
    }

    /**
     * Extracts the display name from an EXTINF line.
     * The display name is the text after the last comma.
     *
     * For example:
     * #EXTINF:-1 tvg-name="BBC" group-title="UK",BBC One HD
     * Returns: "BBC One HD"
     */
    private fun extractDisplayName(extInfLine: String): String {
        // Find the last comma that is NOT inside quotes
        var insideQuotes = false
        var lastCommaIndex = -1

        for (index in extInfLine.indices) {
            when (extInfLine[index]) {
                '"' -> insideQuotes = !insideQuotes
                ',' -> if (!insideQuotes) lastCommaIndex = index
            }
        }

        return if (lastCommaIndex >= 0 && lastCommaIndex < extInfLine.length - 1) {
            extInfLine.substring(lastCommaIndex + 1).trim()
        } else {
            // Fallback: try to get name after the duration part
            val afterColon = extInfLine.substringAfter(":", "")
            val afterDuration = afterColon.substringAfter(" ", afterColon)
            afterDuration.substringAfter(",", afterDuration).trim()
        }
    }

    /**
     * Detects the stream type based on URL extension.
     *
     * @param url Stream URL
     * @return StreamType enum value
     */
    fun detectStreamType(url: String): StreamType {
        val lowerUrl = url.lowercase()
        return when {
            lowerUrl.contains(".m3u8") || lowerUrl.contains("/hls/") -> StreamType.HLS
            lowerUrl.contains(".mpd") -> StreamType.DASH
            lowerUrl.contains(".ts") -> StreamType.MPEG_TS
            else -> StreamType.OTHER
        }
    }
}

/**
 * Enumeration of supported stream types.
 */
enum class StreamType {
    HLS,
    DASH,
    MPEG_TS,
    OTHER
}
