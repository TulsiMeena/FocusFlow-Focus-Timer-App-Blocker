package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Advanced Focus", appName)
  }

  @Test
  fun `test FocusTimerViewModel instantiation`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.timer.FocusTimerViewModel(app)
    org.junit.Assert.assertNotNull(vm)
  }

  @Test
  fun `test FocusBlockOverlayManager instance and hide`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val overlayManager = com.example.shield.FocusBlockOverlayManager(context)
    org.junit.Assert.assertNotNull(overlayManager)
    org.junit.Assert.assertFalse(overlayManager.isShowing)
    overlayManager.hideBlockOverlay()
    org.junit.Assert.assertFalse(overlayManager.isShowing)
  }
}
