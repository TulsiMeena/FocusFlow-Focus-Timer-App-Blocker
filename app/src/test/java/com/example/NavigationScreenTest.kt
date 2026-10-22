package com.example

import com.example.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationScreenTest {

  @Test
  fun testBottomNavItemsInitialization() {
    val items = Screen.bottomNavItems
    assertNotNull("bottomNavItems must not be null", items)
    assertEquals(5, items.size)

    for (screen in items) {
      assertNotNull("Screen instance in bottomNavItems must not be null", screen)
      assertNotNull("Route must not be null", screen.route)
      assertTrue("Route must not be empty", screen.route.isNotEmpty())
      assertNotNull("Title must not be null", screen.title)
      assertTrue("Title must not be empty", screen.title.isNotEmpty())
    }

    assertEquals("home", Screen.Home.route)
    assertEquals("focus", Screen.Focus.route)
    assertEquals("app_blocker", Screen.AppBlocker.route)
    assertEquals("statistics", Screen.Statistics.route)
    assertEquals("settings", Screen.Settings.route)
  }
}
