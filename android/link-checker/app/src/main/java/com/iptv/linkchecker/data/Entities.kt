package com.iptv.linkchecker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SourceType {
    M3U,
    XTREAM,
    MAC_PORTAL
}

@Entity(tableName = "sources")
data class Source(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: SourceType,
    val url: String,
    val username: String = "",
    val password: String = "",
    val macAddress: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val lastCheckedAt: Long? = null,
    val channelCount: Int = 0
)

enum class ChannelStatus {
    UNCHECKED,
    LIVE,
    DEAD,
    SLOW
}

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceId: Long,
    val name: String,
    val group: String = "",
    val streamUrl: String,
    val logoUrl: String = "",
    val status: ChannelStatus = ChannelStatus.UNCHECKED,
    val responseTimeMs: Long = 0,
    val lastCheckedAt: Long? = null,
    val errorMessage: String = ""
)

@Entity(tableName = "ignored_domains")
data class IgnoredDomain(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val addedAt: Long = System.currentTimeMillis()
)
