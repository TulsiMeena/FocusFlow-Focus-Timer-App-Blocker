package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.backup.BackupData
import com.example.backup.BackupManager
import com.example.backup.BackupValidationResult
import com.example.backup.RestoreMode
import com.example.backup.RestoreResult
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BlockedAppEntity
import com.example.data.local.entity.RewardTransactionEntity
import com.example.data.local.entity.SessionRecord
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TimerSettingsEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupAndRestoreUnitTest {

  private lateinit var db: AppDatabase

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  @Throws(IOException::class)
  fun closeDb() {
    db.close()
  }

  @Test
  fun testCreateAndValidateBackupJson() = runBlocking {
    // Populate DB with test records
    db.sessionDao().insertSession(
      SessionRecord(
        sessionId = "backup-sess-1",
        sessionType = "FOCUS",
        plannedDurationSeconds = 1500,
        actualDurationSeconds = 1500,
        startTimestamp = 1000L,
        endTimestamp = 2500L,
        status = "COMPLETED"
      )
    )

    db.taskDao().insertTask(
      TaskEntity(
        title = "Write code",
        notes = "Task notes",
        targetDate = "2026-10-11",
        plannedDurationMinutes = 25
      )
    )

    db.rewardDao().insertReward(
      RewardTransactionEntity(
        sourceId = "backup-sess-1",
        sourceType = "SESSION_COMPLETED",
        points = 50,
        description = "Completed 25m Focus Session"
      )
    )

    db.blockedAppDao().insertBlockedApp(
      BlockedAppEntity(packageName = "com.distracting.app", appName = "Distracting App")
    )

    // Export JSON
    val json = BackupManager.createBackupJson(db)
    assertTrue(json.contains("\"format\": \"ADVANCED_FOCUS_BACKUP\""))
    assertTrue(json.contains("\"version\": 1"))
    assertTrue(json.contains("backup-sess-1"))
    assertTrue(json.contains("Write code"))

    // Validate Exported JSON
    val validation = BackupManager.parseAndValidateBackupJson(json)
    assertTrue("Generated JSON must validate successfully", validation is BackupValidationResult.Success)

    val success = validation as BackupValidationResult.Success
    assertEquals(1, success.summary.sessionsCount)
    assertEquals(1, success.summary.tasksCount)
    assertEquals(1, success.summary.rewardsCount)
    assertEquals(1, success.summary.blockedAppsCount)
  }

  @Test
  fun testCorruptedJsonRejection() {
    val corrupted = "{\"format\": \"ADVANCED_FOCUS_BACKUP\", \"version\": 1, invalid_json..."
    val validation = BackupManager.parseAndValidateBackupJson(corrupted)
    assertTrue(validation is BackupValidationResult.Error)
    val error = validation as BackupValidationResult.Error
    assertTrue(error.message.contains("Corrupted") || error.message.contains("malformed"))
  }

  @Test
  fun testEmptyJsonRejection() {
    val validation = BackupManager.parseAndValidateBackupJson("")
    assertTrue(validation is BackupValidationResult.Error)
    assertEquals("Selected backup file is empty.", (validation as BackupValidationResult.Error).message)
  }

  @Test
  fun testIncompatibleFormatRejection() {
    val invalidFormatJson = """
      {
        "format": "RANDOM_OTHER_APP",
        "version": 1,
        "exportTimestamp": 10000
      }
    """.trimIndent()

    val validation = BackupManager.parseAndValidateBackupJson(invalidFormatJson)
    assertTrue(validation is BackupValidationResult.Error)
    assertTrue((validation as BackupValidationResult.Error).message.contains("Incompatible file format"))
  }

  @Test
  fun testUnsupportedVersionRejection() {
    val futureVersionJson = """
      {
        "format": "ADVANCED_FOCUS_BACKUP",
        "version": 99,
        "exportTimestamp": 10000
      }
    """.trimIndent()

    val validation = BackupManager.parseAndValidateBackupJson(futureVersionJson)
    assertTrue(validation is BackupValidationResult.Error)
    assertTrue((validation as BackupValidationResult.Error).message.contains("Unsupported backup version"))
  }

  @Test
  fun testSettingsSecretsAreExcludedFromExport() = runBlocking {
    // Setup settings with security credentials
    val settingsWithSecrets = TimerSettingsEntity(
      id = 1,
      focusDurationMinutes = 30,
      isSettingsProtectionEnabled = true,
      hashedPin = "secret_hash_abc",
      salt = "secret_salt_xyz",
      recoveryHint = "Secret recovery word"
    )
    db.timerSettingsDao().saveSettings(settingsWithSecrets)

    val json = BackupManager.createBackupJson(db)

    // Secrets MUST NOT be present in export
    assertFalse("Hashed PIN must not be in exported JSON", json.contains("secret_hash_abc"))
    assertFalse("Salt must not be in exported JSON", json.contains("secret_salt_xyz"))
    assertFalse("Recovery hint must not be in exported JSON", json.contains("Secret recovery word"))
  }

  @Test
  fun testRestoreMergeModePreventsDuplicates() = runBlocking {
    val existingSession = SessionRecord(
      sessionId = "existing-sess-1",
      sessionType = "FOCUS",
      plannedDurationSeconds = 1500,
      actualDurationSeconds = 1500,
      startTimestamp = 1000L,
      endTimestamp = 2500L,
      status = "COMPLETED"
    )
    db.sessionDao().insertSession(existingSession)

    val existingReward = RewardTransactionEntity(
      sourceId = "existing-sess-1",
      sourceType = "SESSION_COMPLETED",
      points = 50,
      description = "Completed 25m Focus Session"
    )
    db.rewardDao().insertReward(existingReward)

    // BackupData has the existing session + a new session
    val backupData = BackupData(
      version = 1,
      format = BackupManager.BACKUP_FORMAT,
      settings = null,
      sessions = listOf(
        existingSession,
        SessionRecord(
          sessionId = "new-sess-2",
          sessionType = "FOCUS",
          plannedDurationSeconds = 1500,
          actualDurationSeconds = 1500,
          startTimestamp = 3000L,
          endTimestamp = 4500L,
          status = "COMPLETED"
        )
      ),
      tasks = emptyList(),
      rewards = listOf(
        existingReward,
        RewardTransactionEntity(
          sourceId = "new-sess-2",
          sourceType = "SESSION_COMPLETED",
          points = 50,
          description = "New session reward"
        )
      ),
      blockedApps = emptyList()
    )

    val restoreResult = BackupManager.restoreBackupData(db, backupData, RestoreMode.MERGE)
    assertTrue(restoreResult is RestoreResult.Success)

    val allSessions = db.sessionDao().getAllSessionsDirect()
    // Should contain exactly 2 sessions, NOT 3
    assertEquals(2, allSessions.size)

    val allRewards = db.rewardDao().getAllRewardsDirect()
    // Should contain exactly 2 rewards, NOT 3
    assertEquals(2, allRewards.size)
  }

  @Test
  fun testRestoreReplaceModeReplacesEntireDataSafely() = runBlocking {
    // Populate old session
    db.sessionDao().insertSession(
      SessionRecord(
        sessionId = "old-sess-to-replace",
        sessionType = "FOCUS",
        plannedDurationSeconds = 1500,
        actualDurationSeconds = 1500,
        startTimestamp = 100L,
        endTimestamp = 1600L,
        status = "COMPLETED"
      )
    )

    val backupData = BackupData(
      version = 1,
      format = BackupManager.BACKUP_FORMAT,
      settings = null,
      sessions = listOf(
        SessionRecord(
          sessionId = "replaced-sess-1",
          sessionType = "FOCUS",
          plannedDurationSeconds = 1800,
          actualDurationSeconds = 1800,
          startTimestamp = 5000L,
          endTimestamp = 6800L,
          status = "COMPLETED"
        )
      ),
      tasks = emptyList(),
      rewards = emptyList(),
      blockedApps = emptyList()
    )

    val result = BackupManager.restoreBackupData(db, backupData, RestoreMode.REPLACE)
    assertTrue(result is RestoreResult.Success)

    val allSessions = db.sessionDao().getAllSessionsDirect()
    assertEquals(1, allSessions.size)
    assertEquals("replaced-sess-1", allSessions[0].sessionId)
  }
}
