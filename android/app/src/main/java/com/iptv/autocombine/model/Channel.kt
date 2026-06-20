package com.iptv.autocombine.model

/**
 * Represents a single IPTV channel parsed from an M3U playlist.
 *
 * @property name Display name of the channel
 * @property url Stream URL (HLS, DASH, or direct HTTP)
 * @property logoUrl URL of the channel logo/icon
 * @property group Country or category group (from group-title attribute)
 * @property tvgId EPG identifier (from tvg-id attribute)
 * @property tvgName EPG name (from tvg-name attribute)
 * @property tvgLogo Logo URL from tvg-logo attribute
 * @property language Channel language
 * @property country Country code
 * @property extras Any additional EXTINF attributes not explicitly modeled
 */
data class Channel(
    val name: String,
    val url: String,
    val logoUrl: String = "",
    val group: String = "",
    val tvgId: String = "",
    val tvgName: String = "",
    val tvgLogo: String = "",
    val language: String = "",
    val country: String = "",
    val extras: Map<String, String> = emptyMap()
) {
    /**
     * Unique identifier derived from the channel name and URL.
     * Used for favorites storage.
     */
    val id: String
        get() = "${name}_${url}".hashCode().toString()

    /**
     * Returns the best available logo URL, preferring tvg-logo over logoUrl.
     */
    val effectiveLogoUrl: String
        get() = tvgLogo.ifBlank { logoUrl }

    /**
     * Returns the display name, preferring tvg-name if available.
     */
    val displayName: String
        get() = tvgName.ifBlank { name }

    /**
     * Returns the group name, defaulting to "Uncategorized" if empty.
     */
    val displayGroup: String
        get() = group.ifBlank { "Uncategorized" }
}

/**
 * Represents a group of channels (typically by country).
 *
 * @property name Group display name
 * @property channels List of channels in this group
 * @property isExpanded Whether this group is expanded in the UI
 */
data class ChannelGroup(
    val name: String,
    val channels: List<Channel>,
    var isExpanded: Boolean = true
)

/**
 * Represents the UI state for the channel list screen.
 */
sealed class PlaylistState {
    /** Initial/idle state */
    data object Idle : PlaylistState()

    /** Loading playlist from network */
    data object Loading : PlaylistState()

    /** Playlist loaded successfully */
    data class Success(
        val groups: List<ChannelGroup>,
        val totalChannels: Int
    ) : PlaylistState()

    /** Error loading playlist */
    data class Error(val message: String) : PlaylistState()
}
