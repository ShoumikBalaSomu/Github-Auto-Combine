package com.iptv.autocombine.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.iptv.autocombine.model.Channel
import com.iptv.autocombine.model.ChannelGroup
import com.iptv.autocombine.model.PlaylistState
import com.iptv.autocombine.repository.PlaylistRepository
import com.iptv.autocombine.util.FavoritesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel for the main IPTV application.
 *
 * Manages playlist loading, search, favorites, and auto-refresh.
 * Shared between all fragments (channel list, favorites, settings).
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "iptv_settings"
        private const val KEY_PLAYLIST_URL = "playlist_url"
        private const val KEY_AUTO_REFRESH = "auto_refresh_enabled"
        private const val KEY_REFRESH_INTERVAL = "refresh_interval_minutes"
        private const val DEFAULT_URL =
            "https://shoumikbalasomu.github.io/Github-Auto-Combine/output/combined_by_country.m3u"
        private const val DEFAULT_REFRESH_INTERVAL = 60L // minutes
    }

    private val repository = PlaylistRepository()
    val favoritesManager = FavoritesManager.getInstance(application)
    private val prefs: SharedPreferences =
        application.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

    // --- Playlist State ---
    private val _playlistState = MutableLiveData<PlaylistState>(PlaylistState.Idle)
    val playlistState: LiveData<PlaylistState> = _playlistState

    // --- All channels (flat list) ---
    private val _allChannels = MutableLiveData<List<Channel>>(emptyList())
    val allChannels: LiveData<List<Channel>> = _allChannels

    // --- Grouped channels ---
    private val _channelGroups = MutableLiveData<List<ChannelGroup>>(emptyList())
    val channelGroups: LiveData<List<ChannelGroup>> = _channelGroups

    // --- Filtered/searched channels ---
    private val _filteredGroups = MutableLiveData<List<ChannelGroup>>(emptyList())
    val filteredGroups: LiveData<List<ChannelGroup>> = _filteredGroups

    // --- Search query ---
    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    // --- Favorites ---
    private val _favoriteChannels = MutableLiveData<List<Channel>>(emptyList())
    val favoriteChannels: LiveData<List<Channel>> = _favoriteChannels

    // --- Favorite toggled event (for snackbar) ---
    private val _favoriteEvent = MutableLiveData<Pair<Channel, Boolean>?>()
    val favoriteEvent: LiveData<Pair<Channel, Boolean>?> = _favoriteEvent

    // --- Currently playing channel ---
    private val _currentChannel = MutableLiveData<Channel?>()
    val currentChannel: LiveData<Channel?> = _currentChannel

    // Auto-refresh job
    private var autoRefreshJob: Job? = null

    init {
        loadPlaylist()
        setupAutoRefresh()
    }

    /**
     * Loads the playlist from the configured URL.
     * Updates all state LiveData accordingly.
     */
    fun loadPlaylist() {
        _playlistState.value = PlaylistState.Loading

        viewModelScope.launch {
            try {
                val url = getPlaylistUrl()
                val channels = repository.fetchPlaylist(url)

                _allChannels.value = channels

                val groups = repository.organizeByGroup(channels)
                _channelGroups.value = groups
                _filteredGroups.value = groups

                _playlistState.value = PlaylistState.Success(
                    groups = groups,
                    totalChannels = channels.size
                )

                // Update favorites based on loaded channels
                updateFavorites()

                // Re-apply search if active
                val query = _searchQuery.value
                if (!query.isNullOrBlank()) {
                    applySearch(query)
                }
            } catch (e: Exception) {
                _playlistState.value = PlaylistState.Error(
                    e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    /**
     * Searches channels by the given query.
     * Updates filteredGroups with matching results.
     *
     * @param query Search query string
     */
    fun search(query: String) {
        _searchQuery.value = query
        applySearch(query)
    }

    /**
     * Applies search filter to the current channel groups.
     */
    private fun applySearch(query: String) {
        val allGroups = _channelGroups.value ?: return

        if (query.isBlank()) {
            _filteredGroups.value = allGroups
            return
        }

        val allChannelsList = _allChannels.value ?: return
        val filtered = repository.searchChannels(query, allChannelsList)
        val filteredGrouped = repository.organizeByGroup(filtered)
        _filteredGroups.value = filteredGrouped
    }

    /**
     * Toggles a channel's favorite status.
     *
     * @param channel The channel to toggle
     */
    fun toggleFavorite(channel: Channel) {
        val isNowFavorite = favoritesManager.toggleFavorite(channel)
        _favoriteEvent.value = Pair(channel, isNowFavorite)
        updateFavorites()
    }

    /**
     * Clears the favorite event after it has been consumed.
     */
    fun clearFavoriteEvent() {
        _favoriteEvent.value = null
    }

    /**
     * Updates the favorites list based on current channels and stored favorites.
     */
    fun updateFavorites() {
        val channels = _allChannels.value
        _favoriteChannels.value = if (channels.isNullOrEmpty()) {
            favoritesManager.getStoredFavorites()
        } else {
            favoritesManager.getFavoriteChannels(channels)
        }
    }

    /**
     * Sets the currently playing channel.
     *
     * @param channel The channel to play
     */
    fun setCurrentChannel(channel: Channel) {
        _currentChannel.value = channel
    }

    // --- Settings ---

    /**
     * Returns the configured playlist URL.
     */
    fun getPlaylistUrl(): String {
        return prefs.getString(KEY_PLAYLIST_URL, DEFAULT_URL) ?: DEFAULT_URL
    }

    /**
     * Sets a new playlist URL and reloads the playlist.
     *
     * @param url New M3U playlist URL
     */
    fun setPlaylistUrl(url: String) {
        prefs.edit().putString(KEY_PLAYLIST_URL, url).apply()
        repository.clearCache()
        loadPlaylist()
    }

    /**
     * Returns whether auto-refresh is enabled.
     */
    fun isAutoRefreshEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_REFRESH, true)
    }

    /**
     * Enables or disables auto-refresh.
     *
     * @param enabled Whether to enable auto-refresh
     */
    fun setAutoRefreshEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_REFRESH, enabled).apply()
        setupAutoRefresh()
    }

    /**
     * Returns the auto-refresh interval in minutes.
     */
    fun getRefreshIntervalMinutes(): Long {
        return prefs.getLong(KEY_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL)
    }

    /**
     * Sets the auto-refresh interval.
     *
     * @param minutes Interval in minutes
     */
    fun setRefreshIntervalMinutes(minutes: Long) {
        prefs.edit().putLong(KEY_REFRESH_INTERVAL, minutes).apply()
        setupAutoRefresh()
    }

    /**
     * Sets up or cancels auto-refresh based on current settings.
     */
    private fun setupAutoRefresh() {
        autoRefreshJob?.cancel()

        if (!isAutoRefreshEnabled()) return

        val intervalMs = getRefreshIntervalMinutes() * 60 * 1000

        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(intervalMs)
                loadPlaylist()
            }
        }
    }

    /**
     * Toggles the expansion state of a channel group.
     *
     * @param groupIndex Index of the group in the filtered list
     */
    fun toggleGroupExpansion(groupIndex: Int) {
        val groups = _filteredGroups.value?.toMutableList() ?: return
        if (groupIndex in groups.indices) {
            val group = groups[groupIndex]
            groups[groupIndex] = group.copy(isExpanded = !group.isExpanded)
            _filteredGroups.value = groups
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
