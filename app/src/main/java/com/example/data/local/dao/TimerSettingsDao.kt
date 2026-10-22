package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.TimerSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerSettingsDao {
  @Query("SELECT * FROM timer_settings WHERE id = 1 LIMIT 1")
  fun getSettings(): Flow<TimerSettingsEntity?>

  @Query("SELECT * FROM timer_settings WHERE id = 1 LIMIT 1")
  suspend fun getSettingsDirect(): TimerSettingsEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun saveSettings(settings: TimerSettingsEntity)
}
