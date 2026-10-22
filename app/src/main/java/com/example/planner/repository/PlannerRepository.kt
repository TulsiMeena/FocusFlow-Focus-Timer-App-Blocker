package com.example.planner.repository

import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PlannerRepository(private val taskDao: TaskDao) {

  val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

  fun getTasksForDate(targetDate: String): Flow<List<TaskEntity>> =
    taskDao.getTasksForDate(targetDate)

  suspend fun createTask(
    title: String,
    notes: String,
    targetDate: String,
    plannedDurationMinutes: Int
  ): Long = withContext(Dispatchers.IO) {
    if (title.isBlank()) return@withContext -1L
    val task = TaskEntity(
      title = title.trim(),
      notes = notes.trim(),
      targetDate = targetDate,
      plannedDurationMinutes = plannedDurationMinutes.coerceIn(1, 480),
      isCompleted = false,
      createdTimestamp = System.currentTimeMillis()
    )
    taskDao.insertTask(task)
  }

  suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
    taskDao.updateTask(task)
  }

  suspend fun toggleTaskCompletion(taskId: Long): TaskEntity? = withContext(Dispatchers.IO) {
    val existing = taskDao.getTaskById(taskId) ?: return@withContext null
    val updated = existing.copy(
      isCompleted = !existing.isCompleted,
      completedTimestamp = if (!existing.isCompleted) System.currentTimeMillis() else null
    )
    taskDao.updateTask(updated)
    updated
  }

  suspend fun deleteTask(taskId: Long) = withContext(Dispatchers.IO) {
    taskDao.deleteTask(taskId)
  }

  suspend fun associateSessionWithTask(taskId: Long, sessionId: String) = withContext(Dispatchers.IO) {
    val existing = taskDao.getTaskById(taskId) ?: return@withContext
    val updated = existing.copy(associatedSessionId = sessionId)
    taskDao.updateTask(updated)
  }
}
