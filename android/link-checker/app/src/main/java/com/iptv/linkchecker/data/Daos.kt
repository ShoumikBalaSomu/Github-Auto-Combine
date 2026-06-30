package com.iptv.linkchecker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources ORDER BY addedAt DESC")
    fun getAllSources(): Flow<List<Source>>

    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun getSourceById(id: Long): Source?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: Source): Long

    @Update
    suspend fun updateSource(source: Source)

    @Delete
    suspend fun deleteSource(source: Source)

    @Query("UPDATE sources SET lastCheckedAt = :timestamp, channelCount = :count WHERE id = :sourceId")
    suspend fun updateSourceCheckInfo(sourceId: Long, timestamp: Long, count: Int)
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY name ASC")
    fun getAllChannels(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId ORDER BY name ASC")
    fun getChannelsBySource(sourceId: Long): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE status = :status ORDER BY name ASC")
    fun getChannelsByStatus(status: ChannelStatus): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE status = 'LIVE' ORDER BY name ASC")
    suspend fun getLiveChannels(): List<Channel>

    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%' OR `group` LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchChannels(query: String): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE status = :status AND (name LIKE '%' || :query || '%' OR `group` LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchChannelsByStatus(status: ChannelStatus, query: String): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%' OR `group` LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchAllChannels(query: String): Flow<List<Channel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: Channel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<Channel>)

    @Update
    suspend fun updateChannel(channel: Channel)

    @Query("DELETE FROM channels WHERE sourceId = :sourceId")
    suspend fun deleteChannelsBySource(sourceId: Long)

    @Query("DELETE FROM channels")
    suspend fun deleteAllChannels()

    @Query("SELECT COUNT(*) FROM channels")
    suspend fun getChannelCount(): Int

    @Query("SELECT COUNT(*) FROM channels WHERE status = :status")
    suspend fun getChannelCountByStatus(status: ChannelStatus): Int

    @Query("SELECT * FROM channels WHERE status = 'UNCHECKED' LIMIT :limit")
    suspend fun getUncheckedChannels(limit: Int): List<Channel>

    @Query("SELECT * FROM channels ORDER BY name ASC")
    suspend fun getAllChannelsList(): List<Channel>
}
