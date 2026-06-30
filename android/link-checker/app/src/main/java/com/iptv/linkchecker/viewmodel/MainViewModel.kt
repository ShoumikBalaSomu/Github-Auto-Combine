package com.iptv.linkchecker.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.linkchecker.data.AppDatabase
import com.iptv.linkchecker.data.Channel
import com.iptv.linkchecker.data.ChannelStatus
import com.iptv.linkchecker.data.IgnoredDomain
import com.iptv.linkchecker.data.Repository
import com.iptv.linkchecker.data.Source
import com.iptv.linkchecker.data.SourceType
import com.iptv.linkchecker.network.CheckProgress
import com.iptv.linkchecker.network.LinkChecker
import com.iptv.linkchecker.network.M3uExporter
import com.iptv.linkchecker.network.M3uParser
import com.iptv.linkchecker.network.MacPortalClient
import com.iptv.linkchecker.network.XtreamClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class FilterType {
    ALL, LIVE, DEAD, SLOW
}

data class UiMessage(
    val text: String,
    val isError: Boolean = false,
    val id: Long = System.currentTimeMillis()
)

data class ExportResult(
    val success: Boolean,
    val filePath: String = "",
    val uri: Uri? = null,
    val channelCount: Int = 0,
    val message: String = ""
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = Repository(db)
    private val m3uParser = M3uParser()
    private val xtreamClient = XtreamClient()
    private val macPortalClient = MacPortalClient()
    private val linkChecker = LinkChecker()
    private val m3uExporter = M3uExporter()

    // Sources
    val sources = repository.getAllSources()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Channels with filtering
    private val _currentFilter = MutableStateFlow(FilterType.ALL)
    val currentFilter: StateFlow<FilterType> = _currentFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val channels: StateFlow<List<Channel>> = combine(
        _currentFilter,
        _searchQuery
    ) { filter, query ->
        Pair(filter, query)
    }.flatMapLatest { (filter, query) ->
        if (query.isBlank()) {
            when (filter) {
                FilterType.ALL -> repository.getAllChannels()
                FilterType.LIVE -> repository.getChannelsByStatus(ChannelStatus.LIVE)
                FilterType.DEAD -> repository.getChannelsByStatus(ChannelStatus.DEAD)
                FilterType.SLOW -> repository.getChannelsByStatus(ChannelStatus.SLOW)
            }
        } else {
            when (filter) {
                FilterType.ALL -> repository.searchChannels(query)
                FilterType.LIVE -> repository.searchChannelsByStatus(ChannelStatus.LIVE, query)
                FilterType.DEAD -> repository.searchChannelsByStatus(ChannelStatus.DEAD, query)
                FilterType.SLOW -> repository.searchChannelsByStatus(ChannelStatus.SLOW, query)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Check progress
    val checkProgress: StateFlow<CheckProgress> = linkChecker.progress

    // UI State
    private val _isLoadingSource = MutableStateFlow(false)
    val isLoadingSource: StateFlow<Boolean> = _isLoadingSource.asStateFlow()

    private val _uiMessage = MutableStateFlow<UiMessage?>(null)
    val uiMessage: StateFlow<UiMessage?> = _uiMessage.asStateFlow()

    private val _exportResult = MutableStateFlow<ExportResult?>(null)
    val exportResult: StateFlow<ExportResult?> = _exportResult.asStateFlow()

    private val _channelStats = MutableStateFlow(ChannelStats())
    val channelStats: StateFlow<ChannelStats> = _channelStats.asStateFlow()

    private var checkJob: Job? = null

    // Ignored domains
    val ignoredDomains = repository.getAllIgnoredDomains()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    data class ChannelStats(
        val total: Int = 0,
        val live: Int = 0,
        val dead: Int = 0,
        val slow: Int = 0,
        val unchecked: Int = 0
    )

    init {
        viewModelScope.launch {
            repository.getAllChannels().collect { channelList ->
                _channelStats.value = ChannelStats(
                    total = channelList.size,
                    live = channelList.count { it.status == ChannelStatus.LIVE },
                    dead = channelList.count { it.status == ChannelStatus.DEAD },
                    slow = channelList.count { it.status == ChannelStatus.SLOW },
                    unchecked = channelList.count { it.status == ChannelStatus.UNCHECKED }
                )
            }
        }
    }

    // Source management
    fun addM3uSource(name: String, url: String) {
        viewModelScope.launch {
            _isLoadingSource.value = true
            try {
                val sourceName = name.ifBlank { "M3U Playlist" }
                val source = Source(
                    name = sourceName,
                    type = SourceType.M3U,
                    url = url
                )
                val sourceId = repository.insertSource(source)

                val channels = m3uParser.parseFromUrl(url, sourceId)
                repository.insertChannels(channels)
                repository.updateSourceCheckInfo(sourceId, System.currentTimeMillis(), channels.size)

                _uiMessage.value = UiMessage("Added $sourceName with ${channels.size} channels")
            } catch (e: Exception) {
                _uiMessage.value = UiMessage("Failed to add M3U: ${e.message}", isError = true)
            } finally {
                _isLoadingSource.value = false
            }
        }
    }

    fun addXtreamSource(name: String, serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _isLoadingSource.value = true
            try {
                val sourceName = name.ifBlank { "Xtream: $username" }
                val source = Source(
                    name = sourceName,
                    type = SourceType.XTREAM,
                    url = serverUrl,
                    username = username,
                    password = password
                )
                val sourceId = repository.insertSource(source)

                val channels = xtreamClient.getChannels(serverUrl, username, password, sourceId)
                repository.insertChannels(channels)
                repository.updateSourceCheckInfo(sourceId, System.currentTimeMillis(), channels.size)

                _uiMessage.value = UiMessage("Added $sourceName with ${channels.size} channels")
            } catch (e: Exception) {
                _uiMessage.value = UiMessage("Failed to add Xtream: ${e.message}", isError = true)
            } finally {
                _isLoadingSource.value = false
            }
        }
    }

    fun addMacSource(name: String, portalUrl: String, macAddress: String) {
        viewModelScope.launch {
            _isLoadingSource.value = true
            try {
                val sourceName = name.ifBlank { "MAC: $macAddress" }
                val source = Source(
                    name = sourceName,
                    type = SourceType.MAC_PORTAL,
                    url = portalUrl,
                    macAddress = macAddress
                )
                val sourceId = repository.insertSource(source)

                val channels = macPortalClient.getChannels(portalUrl, macAddress, sourceId)
                repository.insertChannels(channels)
                repository.updateSourceCheckInfo(sourceId, System.currentTimeMillis(), channels.size)

                _uiMessage.value = UiMessage("Added $sourceName with ${channels.size} channels")
            } catch (e: Exception) {
                _uiMessage.value = UiMessage("Failed to add MAC Portal: ${e.message}", isError = true)
            } finally {
                _isLoadingSource.value = false
            }
        }
    }

    fun deleteSource(source: Source) {
        viewModelScope.launch {
            repository.deleteSource(source)
            _uiMessage.value = UiMessage("Deleted ${source.name}")
        }
    }

    fun refreshSource(source: Source) {
        viewModelScope.launch {
            _isLoadingSource.value = true
            try {
                repository.deleteChannelsBySource(source.id)

                val channels = when (source.type) {
                    SourceType.M3U -> m3uParser.parseFromUrl(source.url, source.id)
                    SourceType.XTREAM -> xtreamClient.getChannels(
                        source.url, source.username, source.password, source.id
                    )
                    SourceType.MAC_PORTAL -> macPortalClient.getChannels(
                        source.url, source.macAddress, source.id
                    )
                }

                repository.insertChannels(channels)
                repository.updateSourceCheckInfo(source.id, System.currentTimeMillis(), channels.size)
                _uiMessage.value = UiMessage("Refreshed ${source.name}: ${channels.size} channels")
            } catch (e: Exception) {
                _uiMessage.value = UiMessage("Failed to refresh: ${e.message}", isError = true)
            } finally {
                _isLoadingSource.value = false
            }
        }
    }

    // Link checking
    fun startChecking() {
        if (checkJob?.isActive == true) return

        checkJob = viewModelScope.launch {
            try {
                val allChannels = repository.getAllChannelsList()
                if (allChannels.isEmpty()) {
                    _uiMessage.value = UiMessage("No channels to check. Add sources first.", isError = true)
                    return@launch
                }

                // Load ignored domains
                val ignored = repository.getIgnoredDomainStrings()
                if (ignored.isNotEmpty()) {
                    _uiMessage.value = UiMessage("⏭️ ${ignored.size} ignored domains will auto-pass as live")
                }

                linkChecker.checkChannels(allChannels, ignored) { checkedChannel ->
                    repository.updateChannel(checkedChannel)
                }

                _uiMessage.value = UiMessage("Check complete!")
            } catch (e: Exception) {
                _uiMessage.value = UiMessage("Check failed: ${e.message}", isError = true)
            }
        }
    }

    fun stopChecking() {
        linkChecker.cancel()
        checkJob?.cancel()
        checkJob = null
        _uiMessage.value = UiMessage("Check stopped")
    }

    // Filtering
    fun setFilter(filter: FilterType) {
        _currentFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Export
    fun exportLiveChannels() {
        viewModelScope.launch {
            try {
                val liveChannels = repository.getLiveChannels()
                if (liveChannels.isEmpty()) {
                    _exportResult.value = ExportResult(
                        success = false,
                        message = "No live channels to export"
                    )
                    return@launch
                }

                val content = m3uExporter.exportToM3u8(liveChannels)
                val context = getApplication<Application>()
                val fileName = "iptv_live_${System.currentTimeMillis()}.m3u8"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Use MediaStore for Android 10+
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/x-mpegURL")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }

                    val uri = context.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                    )

                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(content.toByteArray())
                        }

                        _exportResult.value = ExportResult(
                            success = true,
                            uri = uri,
                            channelCount = liveChannels.size,
                            message = "Exported ${liveChannels.size} channels to Downloads/$fileName"
                        )
                    } else {
                        throw Exception("Failed to create file in Downloads")
                    }
                } else {
                    // Legacy storage
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )
                    val file = File(downloadsDir, fileName)
                    withContext(Dispatchers.IO) {
                        file.writeText(content)
                    }

                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )

                    _exportResult.value = ExportResult(
                        success = true,
                        filePath = file.absolutePath,
                        uri = uri,
                        channelCount = liveChannels.size,
                        message = "Exported ${liveChannels.size} channels to Downloads/$fileName"
                    )
                }
            } catch (e: Exception) {
                _exportResult.value = ExportResult(
                    success = false,
                    message = "Export failed: ${e.message}"
                )
            }
        }
    }

    fun getShareIntent(): Intent? {
        val result = _exportResult.value ?: return null
        if (!result.success || result.uri == null) return null

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/x-mpegURL"
            putExtra(Intent.EXTRA_STREAM, result.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, "IPTV Live Channels")
            putExtra(Intent.EXTRA_TEXT, "Exported ${result.channelCount} live IPTV channels")
        }
    }

    fun clearMessage() {
        _uiMessage.value = null
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    // Ignored Domains
    fun addIgnoredDomain(domain: String) {
        if (domain.isBlank()) return
        viewModelScope.launch {
            repository.addIgnoredDomain(domain)
            _uiMessage.value = UiMessage("Added $domain to ignore list")
        }
    }

    fun removeIgnoredDomain(domain: IgnoredDomain) {
        viewModelScope.launch {
            repository.removeIgnoredDomain(domain)
            _uiMessage.value = UiMessage("Removed ${domain.domain} from ignore list")
        }
    }

    override fun onCleared() {
        super.onCleared()
        linkChecker.shutdown()
    }
}
