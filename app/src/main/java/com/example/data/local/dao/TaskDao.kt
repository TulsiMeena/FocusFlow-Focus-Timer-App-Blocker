package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTask(task: TaskEntity): Long

  @Update
  suspend fun updateTask(task: TaskEntity)

  @Query("DELETE FROM tasks WHERE id = :id")
  suspend fun deleteTask(id: Long)

  @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, createdTimestamp DESC")
  fun getAllTasks(): Flow<List<TaskEntity>>

  @Query("SELECT * FROM tasks WHERE targetDate = :targetDate ORDER BY isCompleted ASC, createdTimestamp DESC")
  fun getTasksForDate(targetDate: String): Flow<List<TaskEntity>>

  @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
  suspend fun getTaskById(id: Long): TaskEntity?

  @Query("SELECT * FROM tasks ORDER BY createdTimestamp DESC")
  suspend fun getAllTasksDirect(): List<TaskEntity>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertTasks(tasks: List<TaskEntity>): List<Long>

  @Query("DELETE FROM tasks")
  suspend fun clearAllTasks()
}
