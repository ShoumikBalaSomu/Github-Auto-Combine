package com.iptv.autocombine.repository

import com.iptv.autocombine.model.Channel
import com.iptv.autocombine.model.ChannelGroup
import com.iptv.autocombine.parser.M3UParser
import com.iptv.autocombine.util.CountryFlags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Repository responsible for fetching and processing IPTV playlists.
 *
 * Uses OkHttp for network requests and M3UParser for parsing.
 * All operations are suspending functions meant to be called from coroutines.
 */
class PlaylistRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /** Cache of parsed channels from the last successful fetch */
    private var cachedChannels: List<Channel> = emptyList()

    /** Cache of grouped channels */
    private var cachedGroups: List<ChannelGroup> = emptyList()

    /**
     * Fetches and parses the M3U playlist from the given URL.
     *
     * @param url M3U playlist URL
     * @return List of parsed Channel objects
     * @throws IOException if network request fails
     * @throws IllegalStateException if response body is null
     */
    suspend fun fetchPlaylist(url: String): List<Channel> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "IPTV-AutoCombine-Android/1.0")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("Failed to fetch playlist: HTTP ${response.code}")
        }

        val body = response.body?.string()
            ?: throw IllegalStateException("Empty response body")

        val channels = M3UParser.parse(body)
        cachedChannels = channels
        channels
    }

    /**
     * Organizes channels into groups sorted by group name.
     * Each group gets a flag emoji prefix in its display name.
     *
     * @param channels List of channels to organize
     * @return List of ChannelGroups sorted alphabetically
     */
    fun organizeByGroup(channels: List<Channel>): List<ChannelGroup> {
        val grouped = channels.groupBy { it.displayGroup }

        val groups = grouped.map { (groupName, groupChannels) ->
            val flag = CountryFlags.getFlagEmoji(groupName)
            ChannelGroup(
                name = "$flag $groupName",
                channels = groupChannels.sortedBy { it.displayName.lowercase() }
            )
        }.sortedBy { it.name }

        cachedGroups = groups
        return groups
    }

    /**
     * Searches channels by name, group, or tvg-name.
     *
     * @param query Search query string
     * @param channels List of channels to search within
     * @return Filtered list of matching channels
     */
    fun searchChannels(query: String, channels: List<Channel>): List<Channel> {
        if (query.isBlank()) return channels

        val lowerQuery = query.lowercase().trim()
        return channels.filter { channel ->
            channel.displayName.lowercase().contains(lowerQuery) ||
                channel.group.lowercase().contains(lowerQuery) ||
                channel.tvgName.lowercase().contains(lowerQuery) ||
                channel.tvgId.lowercase().contains(lowerQuery)
        }
    }

    /**
     * Returns the cached list of all channels.
     */
    fun getCachedChannels(): List<Channel> = cachedChannels

    /**
     * Returns the cached list of channel groups.
     */
    fun getCachedGroups(): List<ChannelGroup> = cachedGroups

    /**
     * Clears all cached data.
     */
    fun clearCache() {
        cachedChannels = emptyList()
        cachedGroups = emptyList()
    }
}
