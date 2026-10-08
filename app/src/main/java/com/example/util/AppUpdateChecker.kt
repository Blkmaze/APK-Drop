package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val releaseTag: String = "",
    val releaseName: String = "",
    val releaseNotes: String = "",
    val apkUpdatedAt: String = "",
    val apkDownloadUrl: String = "https://github.com/Blkmaze/APK-Drop/releases/download/latest/APK-Drop.apk",
    val apkSizeBytes: Long = 0L,
    val isManualCheck: Boolean = false
)

class AppUpdateChecker(private val context: Context) {
    private val TAG = "AppUpdateChecker"
    private val PREFS_NAME = "apk_drop_updater_prefs"
    private val KEY_LAST_SEEN_UPDATED_AT = "last_seen_apk_updated_at"
    private val GITHUB_API_URL = "https://api.github.com/repos/Blkmaze/APK-Drop/releases/tags/latest"
    private val DEFAULT_DOWNLOAD_URL = "https://github.com/Blkmaze/APK-Drop/releases/download/latest/APK-Drop.apk"

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getLastSeenUpdatedAt(): String? {
        return prefs.getString(KEY_LAST_SEEN_UPDATED_AT, null)
    }

    fun saveLastSeenUpdatedAt(updatedAt: String) {
        prefs.edit().putString(KEY_LAST_SEEN_UPDATED_AT, updatedAt).apply()
    }

    suspend fun checkForUpdate(isManual: Boolean): AppUpdateInfo = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 15000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "APK-Drop-App")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val tagName = json.optString("tag_name", "latest")
                val releaseName = json.optString("name", "New Update")
                val releaseNotes = json.optString("body", "")

                var assetUpdatedAt = ""
                var downloadUrl = DEFAULT_DOWNLOAD_URL
                var assetSize = 0L

                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true) || name.equals("APK-Drop.apk", ignoreCase = true)) {
                            assetUpdatedAt = asset.optString("updated_at", "")
                            downloadUrl = asset.optString("browser_download_url", DEFAULT_DOWNLOAD_URL)
                            assetSize = asset.optLong("size", 0L)
                            break
                        }
                    }
                }

                if (assetUpdatedAt.isEmpty()) {
                    assetUpdatedAt = json.optString("published_at", "")
                }

                val savedUpdatedAt = getLastSeenUpdatedAt()
                val isNewer = if (savedUpdatedAt.isNullOrEmpty()) {
                    assetUpdatedAt.isNotEmpty()
                } else {
                    assetUpdatedAt.isNotEmpty() && assetUpdatedAt > savedUpdatedAt
                }

                Log.d(TAG, "Update check: assetUpdatedAt=$assetUpdatedAt, savedUpdatedAt=$savedUpdatedAt, isNewer=$isNewer")

                return@withContext AppUpdateInfo(
                    hasUpdate = isNewer,
                    releaseTag = tagName,
                    releaseName = if (releaseName.isBlank()) "Update $tagName" else releaseName,
                    releaseNotes = releaseNotes,
                    apkUpdatedAt = assetUpdatedAt,
                    apkDownloadUrl = downloadUrl.ifEmpty { DEFAULT_DOWNLOAD_URL },
                    apkSizeBytes = assetSize,
                    isManualCheck = isManual
                )
            } else {
                Log.w(TAG, "GitHub API returned HTTP $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates: ${e.message}")
        } finally {
            connection?.disconnect()
        }

        return@withContext AppUpdateInfo(hasUpdate = false, isManualCheck = isManual)
    }
}
