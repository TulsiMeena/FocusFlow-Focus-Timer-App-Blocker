package com.example.schedule

import com.example.data.local.dao.ScheduledFocusDao
import com.example.data.local.entity.ScheduledFocusEntity
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(private val dao: ScheduledFocusDao) {

  val allScheduled: Flow<List<ScheduledFocusEntity>> = dao.getAllScheduled()

  suspend fun addScheduledBlock(
    title: String,
    startTime: String,
    endTime: String,
    daysOfWeek: String,
    isStrictMode: Boolean = true
  ): Long {
    return dao.insert(
      ScheduledFocusEntity(
        title = title,
        startTime = startTime,
        endTime = endTime,
        daysOfWeek = daysOfWeek,
        isEnabled = true,
        isStrictMode = isStrictMode
      )
    )
  }

  suspend fun toggleEnabled(id: Long, isEnabled: Boolean) {
    dao.setEnabled(id, isEnabled)
  }

  suspend fun delete(entity: ScheduledFocusEntity) {
    dao.delete(entity)
  }
}
