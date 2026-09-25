package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apks")
data class ApkItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val packageName: String,
    val versionName: String = "1.0",
    val versionCode: Long = 1,
    val fileSizeBytes: Long = 0,
    val category: String = "Utilities",
    val quickCode: String = "",
    val downloadUrl: String = "",
    val localFilePath: String? = null,
    val isDownloaded: Boolean = false,
    val isExtracted: Boolean = false,
    val isFeatured: Boolean = false,
    val developer: String = "Community",
    val description: String = "",
    val iconUrl: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis()
)
