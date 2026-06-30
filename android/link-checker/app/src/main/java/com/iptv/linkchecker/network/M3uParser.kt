package com.iptv.linkchecker.network

import com.iptv.linkchecker.data.Channel
import com.iptv.linkchecker.data.ChannelStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class M3uParser {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun parseFromUrl(url: String, sourceId: Long): List<Channel> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to download playlist: HTTP ${response.code}")
            }

            val body = response.body?.string() ?: throw Exception("Empty response body")
            parseM3uContent(body, sourceId)
        }

    fun parseM3uContent(content: String, sourceId: Long): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()

        var currentName = ""
        var currentGroup = ""
        var currentLogo = ""

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith("#EXTINF:")) {
                // Parse channel info
                currentName = extractAttribute(trimmed, "tvg-name")
                    ?: extractTitleFromExtinf(trimmed)

                currentGroup = extractAttribute(trimmed, "group-title") ?: ""
                currentLogo = extractAttribute(trimmed, "tvg-logo") ?: ""

            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                // This is the URL line
                if (currentName.isEmpty()) {
                    currentName = trimmed.substringAfterLast("/").substringBefore("?")
                }

                channels.add(
                    Channel(
                        sourceId = sourceId,
                        name = currentName,
                        group = currentGroup,
                        streamUrl = trimmed,
                        logoUrl = currentLogo,
                        status = ChannelStatus.UNCHECKED
                    )
                )

                currentName = ""
                currentGroup = ""
                currentLogo = ""
            }
        }

        return channels
    }

    private fun extractAttribute(line: String, attribute: String): String? {
        val pattern = """$attribute="([^"]*?)"""".toRegex()
        return pattern.find(line)?.groupValues?.getOrNull(1)?.takeIf { it.isNotEmpty() }
    }

    private fun extractTitleFromExtinf(line: String): String {
        // Format: #EXTINF:-1 ...,Channel Name
        val commaIndex = line.lastIndexOf(',')
        return if (commaIndex >= 0 && commaIndex < line.length - 1) {
            line.substring(commaIndex + 1).trim()
        } else {
            "Unknown Channel"
        }
    }
}
