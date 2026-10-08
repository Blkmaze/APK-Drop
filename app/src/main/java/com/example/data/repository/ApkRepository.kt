package com.example.data.repository

import android.content.Context
import com.example.data.db.ApkDao
import com.example.data.model.ApkItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class ApkRepository(
    private val apkDao: ApkDao,
    private val context: Context
) {
    val allApks: Flow<List<ApkItem>> = apkDao.getAllApks()
    val vaultApks: Flow<List<ApkItem>> = apkDao.getVaultApks()
    val featuredApks: Flow<List<ApkItem>> = apkDao.getFeaturedApks()

    suspend fun getByQuickCode(code: String): ApkItem? = withContext(Dispatchers.IO) {
        apkDao.getByQuickCode(code.trim())
    }

    suspend fun getById(id: Long): ApkItem? = withContext(Dispatchers.IO) {
        apkDao.getById(id)
    }

    suspend fun saveApk(item: ApkItem): Long = withContext(Dispatchers.IO) {
        apkDao.insert(item)
    }

    suspend fun updateApk(item: ApkItem) = withContext(Dispatchers.IO) {
        apkDao.update(item)
    }

    suspend fun deleteApk(item: ApkItem) = withContext(Dispatchers.IO) {
        item.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        apkDao.delete(item)
    }

    fun getVaultDirectory(): File {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "apks_vault")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun seedInitialCatalogIfEmpty() = withContext(Dispatchers.IO) {
        try {
            if (apkDao.getCount() > 0) return@withContext
        val initialItems = listOf(
            ApkItem(
                name = "ClipBox",
                packageName = "com.clipbox.player",
                versionName = "v1.0",
                versionCode = 10,
                fileSizeBytes = 28_400_000,
                category = "Streaming",
                quickCode = "375177",
                downloadUrl = "https://raw.githubusercontent.com/DocSquiffy/repo/main/clipbox.apk",
                isFeatured = true,
                developer = "ClipBox Team",
                description = "High performance video player and content streaming tool tailored for Android TV and Firestick remotes."
            ),
            ApkItem(
                name = "Doc Squiffy App",
                packageName = "com.docsquiffy.hub",
                versionName = "v2.4",
                versionCode = 24,
                fileSizeBytes = 19_800_000,
                category = "Streaming",
                quickCode = "859942",
                downloadUrl = "https://raw.githubusercontent.com/DocSquiffy/repo/main/squiffy.apk",
                isFeatured = true,
                developer = "Doc Squiffy",
                description = "Official Doc Squiffy updates, rapid downloader shortcuts, guides, and streaming utilities for Fire TV."
            ),
            ApkItem(
                name = "Sportzfy TV",
                packageName = "com.sportzfy.live",
                versionName = "v32",
                versionCode = 32,
                fileSizeBytes = 24_600_000,
                category = "Sports",
                quickCode = "714370",
                downloadUrl = "https://raw.githubusercontent.com/DocSquiffy/repo/main/sportzfy.apk",
                isFeatured = true,
                developer = "Sportzfy Media",
                description = "Live sports updates, scores, schedules, and global broadcast coverage with Leanback remote navigation."
            ),
            ApkItem(
                name = "UK Turks App",
                packageName = "com.ukturks.media",
                versionName = "v2.0.1",
                versionCode = 201,
                fileSizeBytes = 31_200_000,
                category = "UK TV",
                quickCode = "270435",
                downloadUrl = "https://raw.githubusercontent.com/DocSquiffy/repo/main/ukturks.apk",
                isFeatured = true,
                developer = "UK Turks Team",
                description = "Classic UK TV directory, documentaries, sports highlights, and on-demand player."
            ),
            ApkItem(
                name = "SmartTube Next TV",
                packageName = "com.liskovsoft.videomanager",
                versionName = "v21.84",
                versionCode = 2184,
                fileSizeBytes = 18_500_000,
                category = "Streaming",
                quickCode = "285844",
                downloadUrl = "https://github.com/yuliskov/SmartTube/releases/latest/download/smarttube_beta.apk",
                isFeatured = true,
                developer = "Liskov Soft",
                description = "Advanced open-source YouTube client for Android TV boxes and Fire TV sticks with SponsorBlock and 4K 60FPS support."
            ),
            ApkItem(
                name = "Kodi Media Center",
                packageName = "org.xbmc.kodi",
                versionName = "v21.0 Omega",
                versionCode = 21000,
                fileSizeBytes = 68_400_000,
                category = "Media Players",
                quickCode = "104829",
                downloadUrl = "https://mirrors.kodi.tv/releases/android/arm/kodi-21.0-Omega-armeabi-v7a.apk",
                isFeatured = true,
                developer = "XBMC Foundation",
                description = "Award-winning free and open source home theater media hub for videos, music, and home media servers."
            ),
            ApkItem(
                name = "VLC for Android",
                packageName = "org.videolan.vlc",
                versionName = "v3.5.4",
                versionCode = 30504,
                fileSizeBytes = 36_900_000,
                category = "Media Players",
                quickCode = "552190",
                downloadUrl = "https://get.videolan.org/vlc-android/3.5.4/VLC-Android-3.5.4-armv7.apk",
                isFeatured = false,
                developer = "VideoLAN",
                description = "Plays any video file, disc, device, and network streaming protocol with hardware decoding support."
            ),
            ApkItem(
                name = "Downloader Tool Helper",
                packageName = "info.aftvnews.downloader",
                versionName = "v1.4.4",
                versionCode = 144,
                fileSizeBytes = 4_200_000,
                category = "Utilities",
                quickCode = "733762",
                downloadUrl = "https://aftv.news/downloader.apk",
                isFeatured = false,
                developer = "AFTVnews.com",
                description = "Essential Fire TV browser and file downloader utility that enables direct shortcode downloading."
            ),
            ApkItem(
                name = "TiviMate Companion",
                packageName = "ar.tvplayer.companion",
                versionName = "v4.7.0",
                versionCode = 470,
                fileSizeBytes = 12_800_000,
                category = "IPTV Players",
                quickCode = "919283",
                downloadUrl = "https://tivimate.com/releases/tivimate_companion.apk",
                isFeatured = false,
                developer = "Armobsoft FZE",
                description = "Account management and companion tool for TiviMate IPTV player on Fire TV and Android TV."
            ),
            ApkItem(
                name = "Wolf Launcher TV",
                packageName = "com.wolf.launcher",
                versionName = "v0.1.9",
                versionCode = 19,
                fileSizeBytes = 8_700_000,
                category = "Launcher",
                quickCode = "640192",
                downloadUrl = "https://raw.githubusercontent.com/DocSquiffy/repo/main/wolflauncher.apk",
                isFeatured = false,
                developer = "Wolf Modding",
                description = "Ad-free customizable home launcher for Amazon Fire TV sticks and Google TV boxes."
            ),
            ApkItem(
                name = "Stremio TV",
                packageName = "com.stremio.one",
                versionName = "v1.6.11",
                versionCode = 1611,
                fileSizeBytes = 44_100_000,
                category = "Streaming",
                quickCode = "834190",
                downloadUrl = "https://dl.strem.io/four/v1.6.11/android-tv/stremio_1.6.11-arm.apk",
                isFeatured = false,
                developer = "Stremio LLC",
                description = "Media aggregation and streaming platform organizing movies, TV series, channels, and torrent streams."
            ),
            ApkItem(
                name = "Proton VPN TV",
                packageName = "ch.protonvpn.android",
                versionName = "v5.2.0",
                versionCode = 520,
                fileSizeBytes = 32_500_000,
                category = "VPN",
                quickCode = "481029",
                downloadUrl = "https://protonvpn.com/download/ProtonVPN_armv7.apk",
                isFeatured = false,
                developer = "Proton AG",
                description = "Secure Swiss-based encrypted VPN designed for streaming and protecting IP addresses on Fire TV."
            )
        )

        apkDao.insertAll(initialItems)

        // Also check if any existing APK files in vault directory need to be registered
        val vaultDir = getVaultDirectory()
        val files = vaultDir.listFiles { _, name -> name.endsWith(".apk", ignoreCase = true) }
        files?.forEach { file ->
            val pkg = apkDao.getByPackageName(file.nameWithoutExtension)
            if (pkg == null) {
                apkDao.insert(
                    ApkItem(
                        name = file.nameWithoutExtension.replace("_", " ").capitalizeWords(),
                        packageName = "local." + file.nameWithoutExtension.lowercase().replace(" ", "."),
                        versionName = "Stored",
                        fileSizeBytes = file.length(),
                        category = "Utilities",
                        quickCode = generateRandom6DigitCode(),
                        localFilePath = file.absolutePath,
                        isDownloaded = true,
                        isExtracted = true,
                        description = "Local APK stored in MazZe Tools vault."
                    )
                )
            }
        }
        } catch (e: Exception) {
            android.util.Log.e("ApkRepository", "Error seeding initial catalog", e)
        }
    }

    private fun generateRandom6DigitCode(): String {
        return (100000..999999).random().toString()
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
