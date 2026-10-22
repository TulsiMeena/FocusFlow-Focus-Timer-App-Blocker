package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SessionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertSession(session: SessionRecord): Long

  @Query("SELECT * FROM session_records ORDER BY startTimestamp DESC")
  fun getAllSessions(): Flow<List<SessionRecord>>

  @Query("SELECT * FROM session_records ORDER BY startTimestamp DESC LIMIT :limit")
  fun getRecentSessions(limit: Int): Flow<List<SessionRecord>>

  @Query("SELECT * FROM session_records WHERE sessionId = :sessionId LIMIT 1")
  suspend fun getSessionBySessionId(sessionId: String): SessionRecord?

  @Query("SELECT * FROM session_records ORDER BY startTimestamp DESC")
  suspend fun getAllSessionsDirect(): List<SessionRecord>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertSessions(sessions: List<SessionRecord>): List<Long>

  @Query("DELETE FROM session_records WHERE id = :id")
  suspend fun deleteSession(id: Long)

  @Query("DELETE FROM session_records")
  suspend fun clearAllSessions()
}
