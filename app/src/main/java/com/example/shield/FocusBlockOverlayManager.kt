package com.example.shield

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.navigation.Screen
import com.example.timer.NotificationHelper
import java.util.Locale

/**
 * Manages the system-level WindowManager floating overlay view
 * (TYPE_APPLICATION_OVERLAY) with Midnight Black, Neon Purple, and Electric Cyan accents,
 * 60-second countdown, and automatic redirect to the Focus App.
 */
class FocusBlockOverlayManager(private val context: Context) {

  companion object {
    private const val TAG = "FocusBlockOverlay"
    private const val COUNTDOWN_TOTAL_SECONDS = 60
  }

  private val windowManager: WindowManager =
    context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  private val mainHandler = Handler(Looper.getMainLooper())

  private var overlayView: View? = null
  private var isOverlayAttached = false
  private var currentlyBlockedPackage: String? = null

  private var titleTextView: TextView? = null
  private var subtitleTextView: TextView? = null
  private var appNameTextView: TextView? = null
  private var countdownTextView: TextView? = null
  private var strictBadgeView: TextView? = null

  private var countdownRunnable: Runnable? = null
  private var countdownEndEpoch = 0L
  private var hasRedirected = false

  val isShowing: Boolean
    get() = isOverlayAttached

  val currentPackage: String?
    get() = currentlyBlockedPackage

  fun showBlockOverlay(
    appName: String,
    packageName: String,
    isStrictMode: Boolean
  ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
      Log.w(TAG, "Cannot show WindowManager overlay: SYSTEM_ALERT_WINDOW permission is not granted")
      return
    }

    mainHandler.post {
      try {
        if (isOverlayAttached && overlayView != null) {
          currentlyBlockedPackage = packageName
          updateOverlayText(appName, isStrictMode)
          return@post
        }

        currentlyBlockedPackage = packageName
        hasRedirected = false
        countdownEndEpoch = SystemClock.elapsedRealtime() + (COUNTDOWN_TOTAL_SECONDS * 1000L)

        val view = createOverlayView(appName, packageName, isStrictMode)
        val params = createLayoutParams()

        windowManager.addView(view, params)
        overlayView = view
        isOverlayAttached = true

        startCountdown(appName)
        Log.i(TAG, "Full-screen WindowManager overlay displayed for $appName ($packageName)")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to display WindowManager overlay", e)
      }
    }
  }

  fun hideBlockOverlay() {
    mainHandler.post {
      stopCountdown()
      try {
        if (isOverlayAttached && overlayView != null) {
          windowManager.removeView(overlayView)
          overlayView = null
          isOverlayAttached = false
          currentlyBlockedPackage = null
          titleTextView = null
          subtitleTextView = null
          appNameTextView = null
          countdownTextView = null
          strictBadgeView = null
          Log.i(TAG, "WindowManager overlay dismissed")
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to remove WindowManager overlay", e)
        overlayView = null
        isOverlayAttached = false
        currentlyBlockedPackage = null
      }
    }
  }

  private fun startCountdown(appName: String) {
    stopCountdown()

    countdownRunnable = object : Runnable {
      override fun run() {
        val remaining = ((countdownEndEpoch - SystemClock.elapsedRealtime() + 999) / 1000).toInt().coerceIn(0, COUNTDOWN_TOTAL_SECONDS)
        val minutes = remaining / 60
        val seconds = remaining % 60
        val formatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)
        countdownTextView?.text = formatted

        if (remaining <= 0) {
          if (!hasRedirected) {
            hasRedirected = true
            redirectToFocus(appName)
          }
        } else {
          mainHandler.postDelayed(this, 1000L)
        }
      }
    }

    mainHandler.post(countdownRunnable!!)
  }

  private fun stopCountdown() {
    countdownRunnable?.let { mainHandler.removeCallbacks(it) }
    countdownRunnable = null
  }

  private fun redirectToFocus(appName: String) {
    val focusIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(MainActivity.EXTRA_NAVIGATE_TO, Screen.Focus.route)
    }
    try {
      context.startActivity(focusIntent)
    } catch (e: Exception) {
      NotificationHelper(context).sendRedirectToFocusNotification(appName)
    }
    hideBlockOverlay()
  }

  private fun updateOverlayText(appName: String, isStrictMode: Boolean) {
    appNameTextView?.text = "$appName is Paused"
    strictBadgeView?.visibility = if (isStrictMode) View.VISIBLE else View.GONE
  }

  private fun createOverlayView(
    appName: String,
    packageName: String,
    isStrictMode: Boolean
  ): View {
    val density = context.resources.displayMetrics.density
    fun dp(value: Int): Int = (value * density).toInt()

    // 1. Root full-screen container: Midnight Black canvas
    val rootLayout = FrameLayout(context).apply {
      setBackgroundColor(Color.parseColor("#F907080D")) // Deep Midnight Black
      isClickable = true
      isFocusable = true
      isFocusableInTouchMode = true
    }

    // 2. Centered Card Container
    val cardLayout = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      gravity = Gravity.CENTER_HORIZONTAL
      setPadding(dp(22), dp(26), dp(22), dp(26))

      val cardBg = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(24).toFloat()
        setColor(Color.parseColor("#12141F"))
        setStroke(dp(2), if (isStrictMode) Color.parseColor("#9D4EDD") else Color.parseColor("#00E5FF"))
      }
      background = cardBg
    }

    val cardParams = FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.WRAP_CONTENT
    ).apply {
      gravity = Gravity.CENTER
      marginStart = dp(20)
      marginEnd = dp(20)
    }

    // 3. Status Pill
    val pillView = TextView(context).apply {
      text = if (isStrictMode) "STRICT FOCUS MODE ACTIVE" else "FOCUS MODE ACTIVE • DISTRACTION LOCKED"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
      setTextColor(if (isStrictMode) Color.parseColor("#9D4EDD") else Color.parseColor("#00E5FF"))
      typeface = Typeface.DEFAULT_BOLD
      gravity = Gravity.CENTER
      setPadding(dp(12), dp(4), dp(12), dp(4))

      val pillBg = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(16).toFloat()
        setColor(Color.parseColor("#1A1C29"))
        setStroke(dp(1), if (isStrictMode) Color.parseColor("#9D4EDD") else Color.parseColor("#00E5FF"))
      }
      background = pillBg
    }
    cardLayout.addView(pillView, LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.WRAP_CONTENT,
      LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
      gravity = Gravity.CENTER_HORIZONTAL
      bottomMargin = dp(14)
    })

    // 4. Glowing Lock Emblem
    val badgeContainer = FrameLayout(context).apply {
      val badgeBg = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.parseColor("#261438"))
        setStroke(dp(2), Color.parseColor("#9D4EDD"))
      }
      background = badgeBg
    }
    val badgeParams = LinearLayout.LayoutParams(dp(68), dp(68)).apply {
      gravity = Gravity.CENTER_HORIZONTAL
      bottomMargin = dp(12)
    }

    val iconText = TextView(context).apply {
      text = "🔒"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
      gravity = Gravity.CENTER
    }
    badgeContainer.addView(
      iconText,
      FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
    )
    cardLayout.addView(badgeContainer, badgeParams)

    // 5. Large 60-Second Countdown (MM:SS)
    val timerView = TextView(context).apply {
      text = "01:00"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 36f)
      setTextColor(Color.parseColor("#00E5FF"))
      typeface = Typeface.MONOSPACE
      gravity = Gravity.CENTER
    }
    countdownTextView = timerView
    cardLayout.addView(timerView, LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,
      LinearLayout.LayoutParams.WRAP_CONTENT
    ))

    val autoReturnSub = TextView(context).apply {
      text = "Auto-return timer (60s)"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
      setTextColor(Color.parseColor("#A0A7BD"))
      gravity = Gravity.CENTER
    }
    cardLayout.addView(autoReturnSub, LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,
      LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
      bottomMargin = dp(12)
    })

    // 6. Header: "YOUR FOCUS MATTERS"
    val titleView = TextView(context).apply {
      text = "YOUR FOCUS MATTERS"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
      setTextColor(Color.WHITE)
      typeface = Typeface.DEFAULT_BOLD
      gravity = Gravity.CENTER
      letterSpacing = 0.08f
    }
    titleTextView = titleView
    cardLayout.addView(titleView, LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,
      LinearLayout.LayoutParams.WRAP_CONTENT
    ))

    // 7. Subtitle: "This app is paused while you focus."
    val subView = TextView(context).apply {
      text = "This app is paused while you focus."
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
      setTextColor(Color.parseColor("#00E5FF"))
      typeface = Typeface.DEFAULT_BOLD
      gravity = Gravity.CENTER
    }
    subtitleTextView = subView
    cardLayout.addView(subView, LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,
      LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
      topMargin = dp(4)
      bottomMargin = dp(6)
    })

    // 8. Blocked App Name Label
    val appLabelView = TextView(context).apply {
      text = "$appName is locked"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
      setTextColor(Color.parseColor("#B388FF"))
      gravity = Gravity.CENTER
    }
    appNameTextView = appLabelView
    cardLayout.addView(appLabelView, LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,
      LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
      bottomMargin = dp(18)
    })

    // 9. Primary Action: Return to Focus App Now
    val returnBtn = Button(context).apply {
      text = "Return to Focus App Now"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
      setTextColor(Color.WHITE)
      typeface = Typeface.DEFAULT_BOLD

      val btnBg = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(14).toFloat()
        setColor(Color.parseColor("#8B5CF6"))
      }
      background = btnBg

      setOnClickListener {
        redirectToFocus(appName)
      }
    }
    cardLayout.addView(returnBtn, LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,
      dp(48)
    ))

    // 10. Secondary Action: Go to Home Screen
    val homeBtn = Button(context).apply {
      text = "Go to Phone Home Screen"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
      setTextColor(Color.parseColor("#E0E2EC"))

      val outlineBg = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(14).toFloat()
        setColor(Color.parseColor("#1B1D27"))
        setStroke(dp(1), Color.parseColor("#2F3346"))
      }
      background = outlineBg

      setOnClickListener {
        goToHomeScreen()
        hideBlockOverlay()
      }
    }
    cardLayout.addView(homeBtn, LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,
      dp(46)
    ).apply {
      topMargin = dp(8)
    })

    // Back key handling
    rootLayout.setOnKeyListener { _, keyCode, event ->
      if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
        goToHomeScreen()
        hideBlockOverlay()
        true
      } else {
        false
      }
    }

    rootLayout.addView(cardLayout, cardParams)
    return rootLayout
  }

  private fun createLayoutParams(): WindowManager.LayoutParams {
    val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
      @Suppress("DEPRECATION")
      WindowManager.LayoutParams.TYPE_PHONE
    }

    return WindowManager.LayoutParams(
      WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.MATCH_PARENT,
      windowType,
      WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
      PixelFormat.TRANSLUCENT
    ).apply {
      gravity = Gravity.CENTER
    }
  }

  fun goToHomeScreen() {
    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
      addCategory(Intent.CATEGORY_HOME)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
      context.startActivity(homeIntent)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to launch Home intent", e)
    }
  }
}
