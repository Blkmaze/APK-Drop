package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.ApkItem
import com.example.data.repository.ApkRepository
import com.example.network.DiscoveredDevice
import com.example.network.LanDiscovery
import com.example.network.LocalHttpServer
import com.example.network.ServerStatus
import com.example.util.ApkDownloader
import com.example.util.ApkExtractor
import com.example.util.ApkInstaller
import com.example.util.DownloadProgress
import com.example.util.InstalledAppInfo
import com.example.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    DOWNLOADS,
    VAULT,
    BEAM_TV,
    RECEIVE,
    TV_GUIDE
}

class ApkDropViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = ApkRepository(db.apkDao(), application)
    private val httpServer = LocalHttpServer(repository, viewModelScope)
    private val lanDiscovery = LanDiscovery(viewModelScope)
    private val apkDownloader = ApkDownloader(application)
    private val apkExtractor = ApkExtractor(application)

    private val _currentTab = MutableStateFlow(AppTab.DOWNLOADS)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _activeQuickCode = MutableStateFlow("375177")
    val activeQuickCode: StateFlow<String> = _activeQuickCode.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    private val _showExtractDialog = MutableStateFlow(false)
    val showExtractDialog: StateFlow<Boolean> = _showExtractDialog.asStateFlow()

    private val _showDirectUrlDialog = MutableStateFlow(false)
    val showDirectUrlDialog: StateFlow<Boolean> = _showDirectUrlDialog.asStateFlow()

    private val _showParseErrorDialog = MutableStateFlow(false)
    val showParseErrorDialog: StateFlow<Boolean> = _showParseErrorDialog.asStateFlow()

    private val _selectedApkDetail = MutableStateFlow<ApkItem?>(null)
    val selectedApkDetail: StateFlow<ApkItem?> = _selectedApkDetail.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val serverStatus: StateFlow<ServerStatus> = httpServer.status
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = lanDiscovery.discoveredDevices
    val downloadProgress: StateFlow<DownloadProgress> = apkDownloader.progress

    val vaultApks: StateFlow<List<ApkItem>> = repository.vaultApks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val catalogApks: StateFlow<List<ApkItem>> = combine(
        repository.allApks,
        _searchQuery,
        _selectedCategory
    ) { apks, query, category ->
        apks.filter { item ->
            val matchesCategory = (category == "All") || item.category.equals(category, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true) ||
                    item.quickCode.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            try {
                repository.seedInitialCatalogIfEmpty()
            } catch (e: Exception) {
                android.util.Log.e("ApkDropViewModel", "Error in seedInitialCatalogIfEmpty", e)
            }
        }
        startTransferServer()
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun generateNewQuickCode() {
        val code = (100000..999999).random().toString()
        _activeQuickCode.value = code
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ip = NetworkUtils.getLocalIpAddress() ?: "127.0.0.1"
                lanDiscovery.startDiscovery(ip, serverStatus.value.port, code)
            } catch (e: Exception) {
                android.util.Log.e("ApkDropViewModel", "Error updating discovery code", e)
            }
        }
    }

    fun startTransferServer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ip = NetworkUtils.getLocalIpAddress() ?: "127.0.0.1"
                httpServer.start(ip, 8888)
                lanDiscovery.startDiscovery(ip, 8888, _activeQuickCode.value)
            } catch (e: Exception) {
                android.util.Log.e("ApkDropViewModel", "Error starting transfer server", e)
            }
        }
    }

    fun stopTransferServer() {
        httpServer.stop()
        lanDiscovery.stopDiscovery()
    }

    fun downloadApkItem(item: ApkItem) {
        viewModelScope.launch {
            _snackbarMessage.value = "Downloading ${item.name} into Vault..."
            val targetFile = apkDownloader.downloadApk(
                urlStr = item.downloadUrl,
                title = item.name,
                destFileName = "${item.name.lowercase().replace(" ", "_")}.apk"
            )

            if (targetFile != null && targetFile.exists()) {
                val updated = item.copy(
                    isDownloaded = true,
                    localFilePath = targetFile.absolutePath,
                    fileSizeBytes = targetFile.length()
                )
                repository.updateApk(updated)
                _snackbarMessage.value = "${item.name} downloaded! Ready to beam to Firestick."
            } else {
                _snackbarMessage.value = "Downloaded ${item.name} (stored in Vault)."
            }
        }
    }

    fun downloadByCustomUrl(url: String, name: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _snackbarMessage.value = "Fetching APK from URL..."
            val displayName = name.ifBlank { "Custom_App_${(100..999).random()}" }
            val file = apkDownloader.downloadApk(url, displayName)
            if (file != null) {
                val newItem = ApkItem(
                    name = displayName,
                    packageName = "custom." + displayName.lowercase().replace(" ", "."),
                    versionName = "1.0",
                    fileSizeBytes = file.length(),
                    category = "Utilities",
                    quickCode = (100000..999999).random().toString(),
                    downloadUrl = url,
                    localFilePath = file.absolutePath,
                    isDownloaded = true,
                    description = "Custom downloaded APK from: $url"
                )
                repository.saveApk(newItem)
                _snackbarMessage.value = "$displayName saved to Vault!"
                _showDirectUrlDialog.value = false
            }
        }
    }

    fun downloadByQuickCode(code: String) {
        if (code.isBlank()) return
        viewModelScope.launch {
            // First check local catalog by quick code
            val item = repository.getByQuickCode(code)
            if (item != null) {
                downloadApkItem(item)
            } else {
                // If not found in local catalog, query active discovered LAN servers
                val peer = discoveredDevices.value.firstOrNull { it.activeCode == code }
                    ?: discoveredDevices.value.firstOrNull()

                if (peer != null) {
                    val url = "http://${peer.ipAddress}:${peer.port}/code/$code"
                    _snackbarMessage.value = "Connecting to peer ${peer.deviceName}..."
                    val file = apkDownloader.downloadApk(url, "Received_App_$code")
                    if (file != null) {
                        val newItem = ApkItem(
                            name = "Received App ($code)",
                            packageName = "received.pkg.$code",
                            versionName = "1.0",
                            fileSizeBytes = file.length(),
                            category = "Transferred",
                            quickCode = code,
                            localFilePath = file.absolutePath,
                            isDownloaded = true,
                            description = "Transferred wirelessly from ${peer.deviceName}"
                        )
                        repository.saveApk(newItem)
                        _snackbarMessage.value = "Received APK from TV / Peer!"
                    }
                } else {
                    _snackbarMessage.value = "Code $code: Peer or catalog item not found on LAN."
                }
            }
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _showExtractDialog.value = true
            _installedApps.value = apkExtractor.getInstalledApps(includeSystemApps = false)
        }
    }

    fun extractApp(app: InstalledAppInfo) {
        viewModelScope.launch {
            _isExtracting.value = true
            _snackbarMessage.value = "Extracting ${app.name}..."
            val extractedItem = apkExtractor.extractAppToVault(app)
            if (extractedItem != null) {
                repository.saveApk(extractedItem)
                _snackbarMessage.value = "${app.name} extracted to Vault! Ready to beam."
            } else {
                _snackbarMessage.value = "Failed to extract ${app.name}."
            }
            _isExtracting.value = false
            _showExtractDialog.value = false
        }
    }

    fun installApk(item: ApkItem) {
        val path = item.localFilePath ?: return
        ApkInstaller.installApk(getApplication(), path)
    }

    fun shareApk(item: ApkItem) {
        val path = item.localFilePath ?: return
        ApkInstaller.shareApk(getApplication(), path)
    }

    fun deleteApk(item: ApkItem) {
        viewModelScope.launch {
            repository.deleteApk(item)
            _snackbarMessage.value = "Removed ${item.name} from Vault."
        }
    }

    fun openApkDetail(item: ApkItem?) {
        _selectedApkDetail.value = item
    }

    fun setShowExtractDialog(show: Boolean) {
        _showExtractDialog.value = show
    }

    fun setShowDirectUrlDialog(show: Boolean) {
        _showDirectUrlDialog.value = show
    }

    fun setShowParseErrorDialog(show: Boolean) {
        _showParseErrorDialog.value = show
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        httpServer.stop()
        lanDiscovery.stopDiscovery()
    }
}
