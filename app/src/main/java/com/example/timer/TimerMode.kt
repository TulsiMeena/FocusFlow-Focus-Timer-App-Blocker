package com.example.timer

enum class TimerMode(
  val displayName: String,
  val defaultMinutes: Int
) {
  FOCUS("Deep Focus", 25),
  SHORT_BREAK("Short Break", 5),
  LONG_BREAK("Long Break", 15);

  val presets: List<Int>
    get() = when (this) {
      FOCUS -> listOf(15, 25, 45, 60)
      SHORT_BREAK -> listOf(3, 5, 8, 10)
      LONG_BREAK -> listOf(15, 20, 25, 30)
    }
}
