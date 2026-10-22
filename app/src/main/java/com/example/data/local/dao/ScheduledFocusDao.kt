package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ScheduledFocusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledFocusDao {
  @Query("SELECT * FROM scheduled_focus_blocks ORDER BY id ASC")
  fun getAllScheduled(): Flow<List<ScheduledFocusEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(scheduled: ScheduledFocusEntity): Long

  @Update
  suspend fun update(scheduled: ScheduledFocusEntity)

  @Delete
  suspend fun delete(scheduled: ScheduledFocusEntity)

  @Query("UPDATE scheduled_focus_blocks SET isEnabled = :isEnabled WHERE id = :id")
  suspend fun setEnabled(id: Long, isEnabled: Boolean)
}
