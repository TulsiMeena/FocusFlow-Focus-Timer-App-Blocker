package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.BlockedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppDao {
  @Query("SELECT * FROM blocked_apps ORDER BY appName ASC")
  fun getAllBlockedApps(): Flow<List<BlockedAppEntity>>

  @Query("SELECT * FROM blocked_apps ORDER BY appName ASC")
  suspend fun getAllBlockedAppsDirect(): List<BlockedAppEntity>

  @Query("SELECT packageName FROM blocked_apps")
  fun getBlockedPackageNames(): Flow<List<String>>

  @Query("SELECT packageName FROM blocked_apps")
  suspend fun getBlockedPackageNamesDirect(): List<String>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBlockedApp(app: BlockedAppEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(apps: List<BlockedAppEntity>)

  @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
  suspend fun deleteByPackageName(packageName: String)

  @Query("DELETE FROM blocked_apps")
  suspend fun clearAllBlockedApps()
}
