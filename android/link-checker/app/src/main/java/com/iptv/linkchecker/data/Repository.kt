package com.iptv.linkchecker.data

import kotlinx.coroutines.flow.Flow

class Repository(private val db: AppDatabase) {
    private val sourceDao = db.sourceDao()
    private val channelDao = db.channelDao()

    // Sources
    fun getAllSources(): Flow<List<Source>> = sourceDao.getAllSources()

    suspend fun getSourceById(id: Long): Source? = sourceDao.getSourceById(id)

    suspend fun insertSource(source: Source): Long = sourceDao.insertSource(source)

    suspend fun updateSource(source: Source) = sourceDao.updateSource(source)

    suspend fun deleteSource(source: Source) {
        channelDao.deleteChannelsBySource(source.id)
        sourceDao.deleteSource(source)
    }

    suspend fun updateSourceCheckInfo(sourceId: Long, timestamp: Long, count: Int) =
        sourceDao.updateSourceCheckInfo(sourceId, timestamp, count)

    // Channels
    fun getAllChannels(): Flow<List<Channel>> = channelDao.getAllChannels()

    fun getChannelsBySource(sourceId: Long): Flow<List<Channel>> =
        channelDao.getChannelsBySource(sourceId)

    fun getChannelsByStatus(status: ChannelStatus): Flow<List<Channel>> =
        channelDao.getChannelsByStatus(status)

    suspend fun getLiveChannels(): List<Channel> = channelDao.getLiveChannels()

    fun searchChannels(query: String): Flow<List<Channel>> =
        channelDao.searchAllChannels(query)

    fun searchChannelsByStatus(status: ChannelStatus, query: String): Flow<List<Channel>> =
        channelDao.searchChannelsByStatus(status, query)

    suspend fun insertChannels(channels: List<Channel>) = channelDao.insertChannels(channels)

    suspend fun updateChannel(channel: Channel) = channelDao.updateChannel(channel)

    suspend fun deleteChannelsBySource(sourceId: Long) =
        channelDao.deleteChannelsBySource(sourceId)

    suspend fun deleteAllChannels() = channelDao.deleteAllChannels()

    suspend fun getChannelCount(): Int = channelDao.getChannelCount()

    suspend fun getChannelCountByStatus(status: ChannelStatus): Int =
        channelDao.getChannelCountByStatus(status)

    suspend fun getUncheckedChannels(limit: Int): List<Channel> =
        channelDao.getUncheckedChannels(limit)

    suspend fun getAllChannelsList(): List<Channel> = channelDao.getAllChannelsList()
}
