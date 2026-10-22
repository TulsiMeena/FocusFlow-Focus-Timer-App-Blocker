package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ActiveTimerDao
import com.example.data.local.dao.BlockedAppDao
import com.example.data.local.dao.RewardDao
import com.example.data.local.dao.SessionDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.TimerSettingsDao
import com.example.data.local.entity.ActiveTimerStateEntity
import com.example.data.local.entity.BlockedAppEntity
import com.example.data.local.entity.RewardTransactionEntity
import com.example.data.local.entity.SessionRecord
import com.example.data.local.entity.TaskEntity
import com.example.data.local.dao.ScheduledFocusDao
import com.example.data.local.entity.ScheduledFocusEntity
import com.example.data.local.entity.TimerSettingsEntity

@Database(
  entities = [
    SessionRecord::class,
    TimerSettingsEntity::class,
    ActiveTimerStateEntity::class,
    BlockedAppEntity::class,
    TaskEntity::class,
    RewardTransactionEntity::class,
    ScheduledFocusEntity::class
  ],
  version = 6,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun sessionDao(): SessionDao
  abstract fun timerSettingsDao(): TimerSettingsDao
  abstract fun activeTimerDao(): ActiveTimerDao
  abstract fun blockedAppDao(): BlockedAppDao
  abstract fun taskDao(): TaskDao
  abstract fun rewardDao(): RewardDao
  abstract fun scheduledFocusDao(): ScheduledFocusDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `tasks` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `title` TEXT NOT NULL,
            `notes` TEXT NOT NULL,
            `targetDate` TEXT NOT NULL,
            `plannedDurationMinutes` INTEGER NOT NULL,
            `isCompleted` INTEGER NOT NULL,
            `completedTimestamp` INTEGER,
            `createdTimestamp` INTEGER NOT NULL,
            `associatedSessionId` TEXT
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `reward_transactions` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `sourceId` TEXT NOT NULL,
            `sourceType` TEXT NOT NULL,
            `points` INTEGER NOT NULL,
            `description` TEXT NOT NULL,
            `timestamp` INTEGER NOT NULL
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE UNIQUE INDEX IF NOT EXISTS `index_reward_transactions_sourceId_sourceType` 
          ON `reward_transactions` (`sourceId`, `sourceType`)
          """.trimIndent()
        )
      }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `scheduled_focus_blocks` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `title` TEXT NOT NULL,
            `startTime` TEXT NOT NULL,
            `endTime` TEXT NOT NULL,
            `daysOfWeek` TEXT NOT NULL,
            `isEnabled` INTEGER NOT NULL,
            `isStrictMode` INTEGER NOT NULL
          )
          """.trimIndent()
        )
      }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          "ALTER TABLE `timer_settings` ADD COLUMN `isMasterShieldEnabled` INTEGER NOT NULL DEFAULT 1"
        )
      }
    }

    fun getInstance(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "advanced_focus_database"
        )
          .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
