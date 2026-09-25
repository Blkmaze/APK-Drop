package com.example.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.data.model.ApkItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class InstalledAppInfo(
    val name: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val sourceDir: String,
    val sizeBytes: Long,
    val isSystemApp: Boolean
)

class ApkExtractor(private val context: Context) {

    suspend fun getInstalledApps(includeSystemApps: Boolean = false): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages: List<PackageInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(0)
        }

        val result = mutableListOf<InstalledAppInfo>()
        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            if (!includeSystemApps && isSystem) {
                continue
            }

            // Exclude ourselves
            if (pkg.packageName == context.packageName) continue

            val label = appInfo.loadLabel(pm).toString()
            val sourceDir = appInfo.sourceDir ?: continue
            val file = File(sourceDir)
            val size = if (file.exists()) file.length() else 0L

            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkg.versionCode.toLong()
            }

            result.add(
                InstalledAppInfo(
                    name = label,
                    packageName = pkg.packageName,
                    versionName = pkg.versionName ?: "1.0",
                    versionCode = vCode,
                    sourceDir = sourceDir,
                    sizeBytes = size,
                    isSystemApp = isSystem
                )
            )
        }
        result.sortedBy { it.name.lowercase() }
    }

    suspend fun extractAppToVault(app: InstalledAppInfo): ApkItem? = withContext(Dispatchers.IO) {
        val sourceFile = File(app.sourceDir)
        if (!sourceFile.exists()) return@withContext null

        val vaultDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "apks_vault")
        if (!vaultDir.exists()) vaultDir.mkdirs()

        val safeName = app.name.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val targetFile = File(vaultDir, "${safeName}_v${app.versionName}.apk")

        FileInputStream(sourceFile).use { fis ->
            FileOutputStream(targetFile).use { fos ->
                val buffer = ByteArray(64 * 1024)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    fos.write(buffer, 0, read)
                }
                fos.flush()
            }
        }

        val quickCode = (100000..999999).random().toString()

        return@withContext ApkItem(
            name = app.name,
            packageName = app.packageName,
            versionName = app.versionName,
            versionCode = app.versionCode,
            fileSizeBytes = targetFile.length(),
            category = "Utilities",
            quickCode = quickCode,
            localFilePath = targetFile.absolutePath,
            isDownloaded = true,
            isExtracted = true,
            developer = "Extracted from device",
            description = "Extracted APK ready to transfer to Fire TV or another Android device."
        )
    }
}
