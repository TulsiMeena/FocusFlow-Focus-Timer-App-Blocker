package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ActiveTimerStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveTimerDao {
  @Query("SELECT * FROM active_timer_state WHERE id = 1 LIMIT 1")
  fun getActiveTimerState(): Flow<ActiveTimerStateEntity?>

  @Query("SELECT * FROM active_timer_state WHERE id = 1 LIMIT 1")
  suspend fun getActiveTimerStateDirect(): ActiveTimerStateEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun saveActiveTimerState(state: ActiveTimerStateEntity)

  @Query("DELETE FROM active_timer_state WHERE id = 1")
  suspend fun clearActiveTimerState()
}
