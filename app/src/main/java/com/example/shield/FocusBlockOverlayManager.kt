package com.example.shield

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import com.example.MainActivity
import com.example.navigation.Screen
import com.example.quotes.FocusQuotesRepository
import com.example.timer.NotificationHelper
import java.util.Locale

/**
 * Manages the system-level WindowManager full-screen overlay view
 * (TYPE_APPLICATION_OVERLAY) with an edge-to-edge Cyber-Shield layout,
 * custom Canvas neon progress radar ring, real blocked app icon,
 * 60-second countdown, and automatic redirect to the Focus App.
 */
class FocusBlockOverlayManager(private val context: Context) {

  companion object {
    private const val TAG = "FocusBlockOverlay"
    private const val COUNTDOWN_TOTAL_SECONDS = 60

    @Volatile
    private var activeInstance: FocusBlockOverlayManager? = null

    fun dismissActiveOverlay() {
      activeInstance?.hideBlockOverlay()
    }
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
  private var appIconView: ImageView? = null
  private var radarRingView: NeonProgressRingView? = null

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
    isStrictMode: Boolean,
    lockReasonBadge: String? = null
  ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
      Log.w(TAG, "Cannot show WindowManager overlay: SYSTEM_ALERT_WINDOW permission is not granted")
      return
    }

    mainHandler.post {
      try {
        if (isOverlayAttached && overlayView != null) {
          currentlyBlockedPackage = packageName
          updateOverlayContent(appName, packageName, isStrictMode, lockReasonBadge)
          return@post
        }

        currentlyBlockedPackage = packageName
        hasRedirected = false
        countdownEndEpoch = SystemClock.elapsedRealtime() + (COUNTDOWN_TOTAL_SECONDS * 1000L)

        val view = createFullScreenOverlayView(appName, packageName, isStrictMode, lockReasonBadge)
        val params = createLayoutParams()

        windowManager.addView(view, params)
        overlayView = view
        isOverlayAttached = true
        activeInstance = this

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
          appIconView = null
          radarRingView = null
          if (activeInstance === this) {
            activeInstance = null
          }
          Log.i(TAG, "WindowManager overlay dismissed")
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to remove WindowManager overlay", e)
        overlayView = null
        isOverlayAttached = false
        currentlyBlockedPackage = null
        if (activeInstance === this) {
          activeInstance = null
        }
      }
    }
  }

  private fun startCountdown(appName: String) {
    stopCountdown()

    var lastFormattedText = ""
    countdownRunnable = object : Runnable {
      override fun run() {
        val now = SystemClock.elapsedRealtime()
        val remainingMillis = (countdownEndEpoch - now).coerceAtLeast(0L)
        val remaining = ((remainingMillis + 999) / 1000).toInt().coerceIn(0, COUNTDOWN_TOTAL_SECONDS)
        val minutes = remaining / 60
        val seconds = remaining % 60
        val formatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)
        if (formatted != lastFormattedText) {
          lastFormattedText = formatted
          countdownTextView?.text = formatted
        }

        val fraction = (remainingMillis.toFloat() / (COUNTDOWN_TOTAL_SECONDS * 1000f)).coerceIn(0f, 1f)
        radarRingView?.setProgress(fraction)

        if (remaining <= 0) {
          if (!hasRedirected) {
            hasRedirected = true
            redirectToFocus(appName)
          }
        } else {
          mainHandler.postDelayed(this, 100L)
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
    FocusShieldService.notifyOverlayActionTaken()
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

  private fun updateOverlayContent(
    appName: String,
    packageName: String,
    isStrictMode: Boolean,
    lockReasonBadge: String? = null
  ) {
    appNameTextView?.text = "🔒  $appName is Locked"
    strictBadgeView?.text = lockReasonBadge ?: if (isStrictMode) {
      "⚡ STRICT FOCUS SHIELD ACTIVE"
    } else {
      "🛡️ FOCUS MODE ACTIVE • APP SHIELDED"
    }
    try {
      val icon = context.packageManager.getApplicationIcon(packageName)
      appIconView?.setImageDrawable(icon)
    } catch (_: Exception) {
    }
  }

  private fun createFullScreenOverlayView(
    appName: String,
    packageName: String,
    isStrictMode: Boolean,
    lockReasonBadge: String? = null
  ): View {
    val density = context.resources.displayMetrics.density
    fun dp(value: Int): Int = (value * density).toInt()
    val smartPrefs = SmartShieldPreferencesManager.getInstance(context)

    // 1. True Full-Screen Edge-to-Edge Container
    val rootLayout = FrameLayout(context).apply {
      val bgGradient = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(
          Color.parseColor("#05060B"),
          Color.parseColor("#0B0E1C"),
          Color.parseColor("#06070E")
        )
      )
      background = bgGradient
      isClickable = true
      isFocusable = true
      isFocusableInTouchMode = true
    }

    // 2. Full-Screen Vertical Layout (Space-Between from Top to Bottom)
    val contentColumn = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      gravity = Gravity.CENTER_HORIZONTAL
      setPadding(dp(22), dp(42), dp(22), dp(28))
    }

    val contentParams = FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.MATCH_PARENT
    )

    // 3. Top Status Header Pill
    val pillView = TextView(context).apply {
      text = lockReasonBadge ?: if (isStrictMode) {
        "⚡ STRICT FOCUS SHIELD ACTIVE"
      } else {
        "🛡️ FOCUS MODE ACTIVE • APP SHIELDED"
      }
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
      setTextColor(if (isStrictMode) Color.parseColor("#C77DFF") else Color.parseColor("#00E5FF"))
      typeface = Typeface.DEFAULT_BOLD
      gravity = Gravity.CENTER
      letterSpacing = 0.08f
      setPadding(dp(16), dp(8), dp(16), dp(8))

      val pillBg = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(24).toFloat()
        setColor(Color.parseColor("#141728"))
        setStroke(dp(1), if (isStrictMode) Color.parseColor("#9D4EDD") else Color.parseColor("#00E5FF"))
      }
      background = pillBg
    }
    strictBadgeView = pillView
    contentColumn.addView(
      pillView,
      LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      ).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        bottomMargin = dp(14)
      }
    )

    // 4. Center Radar Ring + Real App Icon + 60s Digital Timer
    val ringSizePx = dp(216)
    val radarContainer = FrameLayout(context)
    val ringView = NeonProgressRingView(context, isStrictMode)
    radarRingView = ringView
    radarContainer.addView(
      ringView,
      FrameLayout.LayoutParams(ringSizePx, ringSizePx, Gravity.CENTER)
    )

    val ringInnerColumn = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      gravity = Gravity.CENTER
    }

    // App Icon Badge inside the ring
    val iconWrapper = FrameLayout(context).apply {
      val circleBg = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.parseColor("#181C30"))
        setStroke(dp(2), Color.parseColor("#9D4EDD"))
      }
      background = circleBg
      setPadding(dp(8), dp(8), dp(8), dp(8))
    }

    val iconImage = ImageView(context).apply {
      scaleType = ImageView.ScaleType.FIT_CENTER
      try {
        val appDrawable = context.packageManager.getApplicationIcon(packageName)
        setImageDrawable(appDrawable)
      } catch (_: Exception) {
        setImageResource(android.R.drawable.ic_lock_idle_lock)
      }
    }
    appIconView = iconImage
    iconWrapper.addView(
      iconImage,
      FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER)
    )
    ringInnerColumn.addView(
      iconWrapper,
      LinearLayout.LayoutParams(dp(58), dp(58)).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        bottomMargin = dp(6)
      }
    )

    // Large 60-Second Countdown (MM:SS)
    val timerView = TextView(context).apply {
      text = "01:00"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 36f)
      setTextColor(Color.parseColor("#00E5FF"))
      typeface = Typeface.MONOSPACE
      paint.isFakeBoldText = true
      gravity = Gravity.CENTER
    }
    countdownTextView = timerView
    ringInnerColumn.addView(
      timerView,
      LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      )
    )

    val autoReturnSub = TextView(context).apply {
      text = "AUTO-REDIRECT TIMER"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
      setTextColor(Color.parseColor("#94A3B8"))
      typeface = Typeface.DEFAULT_BOLD
      letterSpacing = 0.1f
      gravity = Gravity.CENTER
    }
    ringInnerColumn.addView(
      autoReturnSub,
      LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      )
    )

    radarContainer.addView(
      ringInnerColumn,
      FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        Gravity.CENTER
      )
    )

    contentColumn.addView(
      radarContainer,
      LinearLayout.LayoutParams(ringSizePx, ringSizePx).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        bottomMargin = dp(12)
      }
    )

    // 5. Bold Headline & Subtitle
    val titleView = TextView(context).apply {
      text = "YOUR FOCUS MATTERS"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
      setTextColor(Color.WHITE)
      typeface = Typeface.DEFAULT_BOLD
      gravity = Gravity.CENTER
      letterSpacing = 0.08f
    }
    titleTextView = titleView
    contentColumn.addView(
      titleView,
      LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      )
    )

    val subView = TextView(context).apply {
      text = "This app is paused while you focus."
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
      setTextColor(Color.parseColor("#00E5FF"))
      typeface = Typeface.DEFAULT_BOLD
      gravity = Gravity.CENTER
    }
    subtitleTextView = subView
    contentColumn.addView(
      subView,
      LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      ).apply {
        topMargin = dp(3)
        bottomMargin = dp(8)
      }
    )

    // Blocked App Badge
    val appBadge = TextView(context).apply {
      text = "🔒  $appName is Locked"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      setTextColor(Color.WHITE)
      typeface = Typeface.DEFAULT_BOLD
      gravity = Gravity.CENTER
      setPadding(dp(14), dp(6), dp(14), dp(6))

      val badgeBg = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(14).toFloat()
        setColor(Color.parseColor("#191D30"))
        setStroke(dp(1), Color.parseColor("#9D4EDD"))
      }
      background = badgeBg
    }
    appNameTextView = appBadge
    contentColumn.addView(
      appBadge,
      LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      ).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        bottomMargin = dp(12)
      }
    )

    // 6. Motivational Quote Glass Card
    val quote = FocusQuotesRepository.getRandomQuote()
    val quoteCard = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      setPadding(dp(16), dp(12), dp(16), dp(12))
      val qBg = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(16).toFloat()
        setColor(Color.parseColor("#121524"))
        setStroke(dp(1), Color.parseColor("#262B44"))
      }
      background = qBg
    }

    val quoteText = TextView(context).apply {
      text = "“${quote.text}”"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      setTextColor(Color.parseColor("#E2E8F0"))
      setTypeface(typeface, Typeface.ITALIC)
      gravity = Gravity.CENTER
    }
    quoteCard.addView(quoteText)

    val quoteAuthor = TextView(context).apply {
      text = "— ${quote.author}"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
      setTextColor(Color.parseColor("#C77DFF"))
      typeface = Typeface.DEFAULT_BOLD
      gravity = Gravity.END
    }
    quoteCard.addView(
      quoteAuthor,
      LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      ).apply {
        topMargin = dp(4)
      }
    )

    contentColumn.addView(
      quoteCard,
      LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      ).apply {
        bottomMargin = dp(10)
      }
    )

    // 6b. Smart Brain Unlock Challenge Panel (Collapsible)
    var currentChallenge = smartPrefs.generateMathChallenge()
    var typedAnswer = ""

    val challengeContainer = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      visibility = View.GONE
      setPadding(dp(14), dp(12), dp(14), dp(12))
      val chBg = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(16).toFloat()
        setColor(Color.parseColor("#11162B"))
        setStroke(dp(1), Color.parseColor("#00E5FF"))
      }
      background = chBg
    }

    val challengeHeader = TextView(context).apply {
      text = "🧠 SOLVE TO UNLOCK FOR 2 MINUTES"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
      setTextColor(Color.parseColor("#00E5FF"))
      typeface = Typeface.DEFAULT_BOLD
      gravity = Gravity.CENTER
    }
    challengeContainer.addView(challengeHeader)

    val challengeProblemView = TextView(context).apply {
      text = "${currentChallenge.expression}   [ Ans: _ ]"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
      setTextColor(Color.WHITE)
      typeface = Typeface.MONOSPACE
      paint.isFakeBoldText = true
      gravity = Gravity.CENTER
      setPadding(0, dp(6), 0, dp(6))
    }
    challengeContainer.addView(challengeProblemView)

    val challengeStatusView = TextView(context).apply {
      text = "Use keypad below to enter the exact answer"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
      setTextColor(Color.parseColor("#94A3B8"))
      gravity = Gravity.CENTER
    }
    challengeContainer.addView(challengeStatusView)

    fun refreshChallengeDisplay() {
      val shown = if (typedAnswer.isEmpty()) "_" else typedAnswer
      challengeProblemView.text = "${currentChallenge.expression}   [ $shown ]"
    }

    // Compact 2-Row Cyber Keypad inside Overlay (0-9, CLR, Verify)
    val row1 = LinearLayout(context).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER
    }
    val row2 = LinearLayout(context).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER
    }

    fun makeKeyButton(label: String, onClick: () -> Unit): Button {
      return Button(context).apply {
        text = label
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(Color.WHITE)
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, 0, 0, 0)
        background = GradientDrawable().apply {
          shape = GradientDrawable.RECTANGLE
          cornerRadius = dp(8).toFloat()
          setColor(Color.parseColor("#1E2540"))
          setStroke(dp(1), Color.parseColor("#354068"))
        }
        setOnClickListener { onClick() }
      }
    }

    listOf("1", "2", "3", "4", "5", "CLR").forEach { key ->
      val btn = makeKeyButton(key) {
        if (key == "CLR") {
          typedAnswer = ""
        } else if (typedAnswer.length < 5) {
          typedAnswer += key
        }
        refreshChallengeDisplay()
      }
      row1.addView(
        btn,
        LinearLayout.LayoutParams(0, dp(36), 1f).apply {
          setMargins(dp(2), dp(4), dp(2), dp(2))
        }
      )
    }

    listOf("6", "7", "8", "9", "0", "⌫").forEach { key ->
      val btn = makeKeyButton(key) {
        if (key == "⌫") {
          if (typedAnswer.isNotEmpty()) typedAnswer = typedAnswer.dropLast(1)
        } else if (typedAnswer.length < 5) {
          typedAnswer += key
        }
        refreshChallengeDisplay()
      }
      row2.addView(
        btn,
        LinearLayout.LayoutParams(0, dp(36), 1f).apply {
          setMargins(dp(2), dp(2), dp(2), dp(6))
        }
      )
    }

    challengeContainer.addView(row1)
    challengeContainer.addView(row2)

    val verifyBtn = Button(context).apply {
      text = "✓ VERIFY & UNLOCK 2 MINS"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      setTextColor(Color.BLACK)
      typeface = Typeface.DEFAULT_BOLD
      background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(10).toFloat()
        setColor(Color.parseColor("#00E5FF"))
      }
      setOnClickListener {
        val entered = typedAnswer.toIntOrNull()
        if (entered != null && entered == currentChallenge.expectedAnswer) {
          smartPrefs.grantTemporaryUnlock(packageName, 2)
          hideBlockOverlay()
        } else {
          challengeStatusView.text = "❌ Wrong answer! Try again: ${currentChallenge.hint}"
          challengeStatusView.setTextColor(Color.parseColor("#FF6B6B"))
          typedAnswer = ""
          refreshChallengeDisplay()
        }
      }
    }
    challengeContainer.addView(
      verifyBtn,
      LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        dp(38)
      )
    )

    contentColumn.addView(
      challengeContainer,
      LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      ).apply {
        bottomMargin = dp(8)
      }
    )

    // Flexible Spacer to push action buttons cleanly toward bottom of full screen
    contentColumn.addView(
      Space(context),
      LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        0,
        1f
      )
    )

    // 7. Full-Screen Bottom Action Buttons
    val returnBtn = Button(context).apply {
      text = "←  RETURN TO FOCUS APP NOW"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
      setTextColor(Color.WHITE)
      typeface = Typeface.DEFAULT_BOLD
      letterSpacing = 0.04f

      val btnBg = GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT,
        intArrayOf(
          Color.parseColor("#7C3AED"),
          Color.parseColor("#9D4EDD")
        )
      ).apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(16).toFloat()
      }
      background = btnBg

      setOnClickListener {
        redirectToFocus(appName)
      }
    }
    contentColumn.addView(
      returnBtn,
      LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        dp(52)
      )
    )

    val homeBtn = Button(context).apply {
      text = "⌂  GO TO PHONE HOME SCREEN"
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
      setTextColor(Color.parseColor("#E2E8F0"))
      typeface = Typeface.DEFAULT_BOLD

      val outlineBg = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(16).toFloat()
        setColor(Color.parseColor("#141829"))
        setStroke(dp(1), Color.parseColor("#2E3654"))
      }
      background = outlineBg

      setOnClickListener {
        goToHomeScreen()
        hideBlockOverlay()
      }
    }
    contentColumn.addView(
      homeBtn,
      LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        dp(48)
      ).apply {
        topMargin = dp(8)
      }
    )

    if (smartPrefs.isSmartChallengeEnabled.value) {
      val challengeToggleBtn = Button(context).apply {
        text = "🧠  SMART UNLOCK CHALLENGE (2 MIN ACCESS)"
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        setTextColor(Color.parseColor("#00E5FF"))
        typeface = Typeface.DEFAULT_BOLD

        background = GradientDrawable().apply {
          shape = GradientDrawable.RECTANGLE
          cornerRadius = dp(14).toFloat()
          setColor(Color.parseColor("#101526"))
          setStroke(dp(1), Color.parseColor("#00E5FF"))
        }

        setOnClickListener {
          if (challengeContainer.visibility == View.VISIBLE) {
            challengeContainer.visibility = View.GONE
            quoteCard.visibility = View.VISIBLE
          } else {
            currentChallenge = smartPrefs.generateMathChallenge()
            typedAnswer = ""
            refreshChallengeDisplay()
            challengeContainer.visibility = View.VISIBLE
            quoteCard.visibility = View.GONE
          }
        }
      }
      contentColumn.addView(
        challengeToggleBtn,
        LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          dp(42)
        ).apply {
          topMargin = dp(8)
        }
      )
    }

    // Hardware Back key handling
    rootLayout.setOnKeyListener { _, keyCode, event ->
      if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
        goToHomeScreen()
        hideBlockOverlay()
        true
      } else {
        false
      }
    }

    rootLayout.addView(contentColumn, contentParams)
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
      gravity = Gravity.FILL
    }
  }

  fun goToHomeScreen() {
    FocusShieldService.notifyOverlayActionTaken()
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

  /**
   * Custom View that renders a glowing Neon Purple & Electric Cyan
   * circular countdown progress ring with cyber radar accents.
   */
  private class NeonProgressRingView(
    context: Context,
    private val isStrict: Boolean
  ) : View(context) {

    private var progressFraction = 1f
    private val density = context.resources.displayMetrics.density
    private val strokePx = 9f * density
    private val outerDashStrokePx = 2f * density
    private val arcRect = RectF()
    private val outerRect = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = strokePx
      color = Color.parseColor("#242942")
    }

    private val outerRadarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = outerDashStrokePx
      color = if (isStrict) Color.parseColor("#559D4EDD") else Color.parseColor("#5500E5FF")
      pathEffect = DashPathEffect(floatArrayOf(12f * density, 10f * density), 0f)
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = strokePx
      strokeCap = Paint.Cap.ROUND
    }

    private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
    }

    fun setProgress(fraction: Float) {
      progressFraction = fraction.coerceIn(0f, 1f)
      invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
      super.onSizeChanged(w, h, oldw, oldh)
      val pad = strokePx * 1.8f
      arcRect.set(pad, pad, w - pad, h - pad)
      val outerPad = outerDashStrokePx * 2f
      outerRect.set(outerPad, outerPad, w - outerPad, h - outerPad)

      val cx = w / 2f
      val cy = h / 2f
      progressPaint.shader = SweepGradient(
        cx,
        cy,
        intArrayOf(
          Color.parseColor("#00E5FF"),
          Color.parseColor("#9D4EDD"),
          Color.parseColor("#00E5FF")
        ),
        floatArrayOf(0f, 0.5f, 1f)
      )

      if (w > 0 && h > 0) {
        auraPaint.shader = RadialGradient(
          cx,
          cy,
          w / 2f,
          intArrayOf(
            Color.parseColor("#289D4EDD"),
            Color.parseColor("#1000E5FF"),
            Color.TRANSPARENT
          ),
          floatArrayOf(0f, 0.65f, 1f),
          Shader.TileMode.CLAMP
        )
      }
    }

    override fun onDraw(canvas: Canvas) {
      super.onDraw(canvas)
      val cx = width / 2f
      val cy = height / 2f
      canvas.drawCircle(cx, cy, width / 2f, auraPaint)
      canvas.drawOval(outerRect, outerRadarPaint)
      canvas.drawOval(arcRect, trackPaint)
      canvas.drawArc(arcRect, -90f, 360f * progressFraction, false, progressPaint)
    }
  }
}
