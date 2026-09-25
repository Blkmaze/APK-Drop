package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ApkItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ApkDao {
    @Query("SELECT * FROM apks ORDER BY addedTimestamp DESC")
    fun getAllApks(): Flow<List<ApkItem>>

    @Query("SELECT * FROM apks WHERE isDownloaded = 1 OR isExtracted = 1 ORDER BY addedTimestamp DESC")
    fun getVaultApks(): Flow<List<ApkItem>>

    @Query("SELECT * FROM apks WHERE isFeatured = 1 ORDER BY addedTimestamp ASC")
    fun getFeaturedApks(): Flow<List<ApkItem>>

    @Query("SELECT * FROM apks WHERE quickCode = :code LIMIT 1")
    suspend fun getByQuickCode(code: String): ApkItem?

    @Query("SELECT * FROM apks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ApkItem?

    @Query("SELECT * FROM apks WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackageName(packageName: String): ApkItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ApkItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ApkItem>)

    @Update
    suspend fun update(item: ApkItem)

    @Delete
    suspend fun delete(item: ApkItem)

    @Query("DELETE FROM apks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
