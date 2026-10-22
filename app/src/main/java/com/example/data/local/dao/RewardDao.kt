package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.RewardTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardDao {
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertReward(reward: RewardTransactionEntity): Long

  @Query("SELECT * FROM reward_transactions ORDER BY timestamp DESC")
  fun getAllRewards(): Flow<List<RewardTransactionEntity>>

  @Query("SELECT COALESCE(SUM(points), 0) FROM reward_transactions")
  fun getTotalPointsFlow(): Flow<Int>

  @Query("SELECT COALESCE(SUM(points), 0) FROM reward_transactions")
  suspend fun getTotalPointsDirect(): Int

  @Query("SELECT COUNT(*) > 0 FROM reward_transactions WHERE sourceId = :sourceId AND sourceType = :sourceType")
  suspend fun hasReward(sourceId: String, sourceType: String): Boolean

  @Query("SELECT * FROM reward_transactions ORDER BY timestamp DESC")
  suspend fun getAllRewardsDirect(): List<RewardTransactionEntity>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertRewards(rewards: List<RewardTransactionEntity>): List<Long>

  @Query("DELETE FROM reward_transactions")
  suspend fun clearAllRewards()
}
