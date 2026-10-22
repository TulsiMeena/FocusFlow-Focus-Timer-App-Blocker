package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ActiveTimerStateEntity
import com.example.data.local.entity.SessionRecord
import com.example.data.local.entity.TimerSettingsEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDatabaseTest {

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
  fun testInsertAndRetrieveSessionRecord() = runBlocking {
    val record = SessionRecord(
      sessionId = "uuid-session-101",
      sessionType = "FOCUS",
      plannedDurationSeconds = 1500,
      actualDurationSeconds = 1500,
      startTimestamp = 1_000_000L,
      endTimestamp = 2_500_000L,
      status = "COMPLETED",
      dateFormatted = "Oct 10, 2026"
    )

    val rowId = db.sessionDao().insertSession(record)
    assert(rowId > 0)

    val retrieved = db.sessionDao().getSessionBySessionId("uuid-session-101")
    assertNotNull(retrieved)
    assertEquals("FOCUS", retrieved?.sessionType)
    assertEquals("COMPLETED", retrieved?.status)
    assertEquals(1500, retrieved?.actualDurationSeconds)

    // Verify duplicate insertion with same sessionId is ignored (returns -1)
    val duplicateRowId = db.sessionDao().insertSession(record)
    assertEquals(-1L, duplicateRowId)
  }

  @Test
  fun testActiveTimerStatePersistenceAndClear() = runBlocking {
    val activeState = ActiveTimerStateEntity(
      id = 1,
      sessionId = "uuid-active-202",
      mode = "FOCUS",
      status = "RUNNING",
      totalDurationSeconds = 1500,
      remainingSeconds = 1200,
      startTimeMillis = 1000L,
      targetEndTimeMillis = 1_201_000L
    )

    db.activeTimerDao().saveActiveTimerState(activeState)

    val restored = db.activeTimerDao().getActiveTimerStateDirect()
    assertNotNull(restored)
    assertEquals("RUNNING", restored?.status)
    assertEquals(1200, restored?.remainingSeconds)

    db.activeTimerDao().clearActiveTimerState()
    val afterClear = db.activeTimerDao().getActiveTimerStateDirect()
    assertNull(afterClear)
  }

  @Test
  fun testTimerSettingsPersistence() = runBlocking {
    val settings = TimerSettingsEntity(
      id = 1,
      focusDurationMinutes = 45,
      shortBreakMinutes = 10,
      longBreakMinutes = 20,
      autoStartBreaks = false,
      autoStartFocus = false
    )

    db.timerSettingsDao().saveSettings(settings)

    val saved = db.timerSettingsDao().getSettingsDirect()
    assertNotNull(saved)
    assertEquals(45, saved?.focusDurationMinutes)
    assertEquals(10, saved?.shortBreakMinutes)
  }

  @Test
  fun testBlockedAppPersistenceAndRemoval() = runBlocking {
    val blockedApp = com.example.data.local.entity.BlockedAppEntity(
      packageName = "com.instagram.android",
      appName = "Instagram"
    )

    db.blockedAppDao().insertBlockedApp(blockedApp)

    val packages = db.blockedAppDao().getBlockedPackageNamesDirect()
    assertEquals(1, packages.size)
    assertEquals("com.instagram.android", packages[0])

    val allApps = db.blockedAppDao().getAllBlockedAppsDirect()
    assertEquals(1, allApps.size)
    assertEquals("Instagram", allApps[0].appName)

    // Delete single app
    db.blockedAppDao().deleteByPackageName("com.instagram.android")
    val afterDelete = db.blockedAppDao().getBlockedPackageNamesDirect()
    assertTrue(afterDelete.isEmpty())
  }

  @Test
  fun testStrictModePersistence() = runBlocking {
    val settings = TimerSettingsEntity(
      id = 1,
      isStrictModeEnabled = true
    )
    db.timerSettingsDao().saveSettings(settings)

    val saved = db.timerSettingsDao().getSettingsDirect()
    assertNotNull(saved)
    assertEquals(true, saved?.isStrictModeEnabled)
  }

  @Test
  fun testTaskDaoPersistenceAndToggle() = runBlocking {
    val task = com.example.data.local.entity.TaskEntity(
      title = "Write report",
      notes = "Chapter 1",
      targetDate = "2026-10-11",
      plannedDurationMinutes = 45
    )

    val id = db.taskDao().insertTask(task)
    assertTrue(id > 0)

    val retrieved = db.taskDao().getTaskById(id)
    assertNotNull(retrieved)
    assertEquals("Write report", retrieved?.title)
    assertEquals(45, retrieved?.plannedDurationMinutes)
    assertFalse(retrieved?.isCompleted == true)

    // Complete task
    db.taskDao().updateTask(retrieved!!.copy(isCompleted = true))
    val completed = db.taskDao().getTaskById(id)
    assertTrue(completed?.isCompleted == true)

    // Delete task
    db.taskDao().deleteTask(id)
    val deleted = db.taskDao().getTaskById(id)
    assertNull(deleted)
  }

  @Test
  fun testRewardDaoDuplicatePreventionAndTotalPoints() = runBlocking {
    val reward1 = com.example.data.local.entity.RewardTransactionEntity(
      sourceId = "session-test-1",
      sourceType = "SESSION_COMPLETED",
      points = 50,
      description = "Completed 25m Focus Session"
    )

    val row1 = db.rewardDao().insertReward(reward1)
    assertTrue("Initial reward insertion should succeed", row1 > 0)

    // Duplicate insert for same sourceId and sourceType MUST be ignored
    val duplicateRow = db.rewardDao().insertReward(reward1)
    assertEquals("Duplicate reward insertion must return -1 (ignored)", -1L, duplicateRow)

    val totalPoints = db.rewardDao().getTotalPointsDirect()
    assertEquals("Total points should only count the unique transaction", 50, totalPoints)

    // Another distinct transaction
    val reward2 = com.example.data.local.entity.RewardTransactionEntity(
      sourceId = "task-test-2",
      sourceType = "TASK_COMPLETED",
      points = 25,
      description = "Completed Task"
    )
    db.rewardDao().insertReward(reward2)

    val updatedTotal = db.rewardDao().getTotalPointsDirect()
    assertEquals(75, updatedTotal)
  }
}

