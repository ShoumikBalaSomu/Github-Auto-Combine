package com.iptv.autocombine.util

import android.content.Context
import android.content.SharedPreferences
import com.iptv.autocombine.model.Channel

/**
 * Manages favorite channels using SharedPreferences.
 *
 * Stores channel IDs as a set of strings for quick lookup.
 * Thread-safe through synchronized access to SharedPreferences.
 */
class FavoritesManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "iptv_favorites"
        private const val KEY_FAVORITES = "favorite_channel_ids"

        @Volatile
        private var instance: FavoritesManager? = null

        /**
         * Returns a singleton instance of FavoritesManager.
         */
        fun getInstance(context: Context): FavoritesManager {
            return instance ?: synchronized(this) {
                instance ?: FavoritesManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns the set of all favorite channel IDs.
     */
    private fun getFavoriteIds(): MutableSet<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    /**
     * Checks if a channel is marked as favorite.
     *
     * @param channel The channel to check
     * @return true if the channel is a favorite
     */
    fun isFavorite(channel: Channel): Boolean {
        return getFavoriteIds().contains(channel.id)
    }

    /**
     * Adds a channel to favorites.
     *
     * @param channel The channel to add
     */
    fun addFavorite(channel: Channel) {
        val ids = getFavoriteIds()
        ids.add(channel.id)
        prefs.edit().putStringSet(KEY_FAVORITES, ids).apply()

        // Also store channel data for reconstruction
        storeChannelData(channel)
    }

    /**
     * Removes a channel from favorites.
     *
     * @param channel The channel to remove
     */
    fun removeFavorite(channel: Channel) {
        val ids = getFavoriteIds()
        ids.remove(channel.id)
        prefs.edit().putStringSet(KEY_FAVORITES, ids).apply()

        // Remove stored channel data
        removeChannelData(channel)
    }

    /**
     * Toggles the favorite state of a channel.
     *
     * @param channel The channel to toggle
     * @return true if the channel is now a favorite, false if it was removed
     */
    fun toggleFavorite(channel: Channel): Boolean {
        return if (isFavorite(channel)) {
            removeFavorite(channel)
            false
        } else {
            addFavorite(channel)
            true
        }
    }

    /**
     * Filters a list of channels to only include favorites.
     *
     * @param allChannels Complete list of channels
     * @return List containing only channels marked as favorites
     */
    fun getFavoriteChannels(allChannels: List<Channel>): List<Channel> {
        val ids = getFavoriteIds()
        return allChannels.filter { ids.contains(it.id) }
    }

    /**
     * Gets the count of favorite channels.
     */
    fun getFavoriteCount(): Int {
        return getFavoriteIds().size
    }

    /**
     * Stores individual channel data for offline reconstruction.
     * Each channel's data is stored as individual preference keys.
     */
    private fun storeChannelData(channel: Channel) {
        val prefix = "channel_${channel.id}_"
        prefs.edit()
            .putString("${prefix}name", channel.name)
            .putString("${prefix}url", channel.url)
            .putString("${prefix}logo", channel.effectiveLogoUrl)
            .putString("${prefix}group", channel.group)
            .putString("${prefix}tvg_id", channel.tvgId)
            .putString("${prefix}tvg_name", channel.tvgName)
            .apply()
    }

    /**
     * Removes stored channel data.
     */
    private fun removeChannelData(channel: Channel) {
        val prefix = "channel_${channel.id}_"
        prefs.edit()
            .remove("${prefix}name")
            .remove("${prefix}url")
            .remove("${prefix}logo")
            .remove("${prefix}group")
            .remove("${prefix}tvg_id")
            .remove("${prefix}tvg_name")
            .apply()
    }

    /**
     * Reconstructs a Channel from stored SharedPreferences data.
     * Used when the full channel list isn't available yet.
     *
     * @param channelId The channel ID to reconstruct
     * @return Reconstructed Channel or null if data is missing
     */
    fun getStoredChannel(channelId: String): Channel? {
        val prefix = "channel_${channelId}_"
        val name = prefs.getString("${prefix}name", null) ?: return null
        val url = prefs.getString("${prefix}url", null) ?: return null

        return Channel(
            name = name,
            url = url,
            logoUrl = prefs.getString("${prefix}logo", "") ?: "",
            group = prefs.getString("${prefix}group", "") ?: "",
            tvgId = prefs.getString("${prefix}tvg_id", "") ?: "",
            tvgName = prefs.getString("${prefix}tvg_name", "") ?: ""
        )
    }

    /**
     * Returns all stored favorite channels (from SharedPreferences data).
     * Useful when the playlist hasn't loaded yet.
     */
    fun getStoredFavorites(): List<Channel> {
        val ids = getFavoriteIds()
        return ids.mapNotNull { getStoredChannel(it) }
    }
}
