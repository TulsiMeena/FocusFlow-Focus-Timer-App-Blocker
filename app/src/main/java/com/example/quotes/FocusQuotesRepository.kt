package com.example.quotes

data class FocusQuote(
  val text: String,
  val author: String,
  val category: String
)

object FocusQuotesRepository {
  val QUOTES = listOf(
    FocusQuote(
      text = "Deep work is the ability to focus without distraction on a cognitively demanding task.",
      author = "Cal Newport",
      category = "Productivity"
    ),
    FocusQuote(
      text = "You have power over your mind - not outside events. Realize this, and you will find strength.",
      author = "Marcus Aurelius",
      category = "Stoicism"
    ),
    FocusQuote(
      text = "You do not rise to the level of your goals. You fall to the level of your systems.",
      author = "James Clear",
      category = "Habits"
    ),
    FocusQuote(
      text = "Focus is saying no to the hundred other good ideas that there are.",
      author = "Steve Jobs",
      category = "Focus"
    ),
    FocusQuote(
      text = "It is not that we have a short time to live, but that we waste a lot of it.",
      author = "Seneca",
      category = "Wisdom"
    ),
    FocusQuote(
      text = "The successful warrior is the average person, with laser-like focus.",
      author = "Bruce Lee",
      category = "Discipline"
    ),
    FocusQuote(
      text = "Do not wait; the time will never be 'just right.' Start where you stand.",
      author = "Napoleon Hill",
      category = "Action"
    ),
    FocusQuote(
      text = "Simplicity boils down to two steps: Identify the essential. Eliminate the rest.",
      author = "Leo Babauta",
      category = "Minimalism"
    ),
    FocusQuote(
      text = "Action is the foundational key to all success.",
      author = "Pablo Picasso",
      category = "Execution"
    )
  )

  fun getRandomQuote(): FocusQuote {
    return QUOTES.random()
  }

  fun getDailyQuote(): FocusQuote {
    val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
    return QUOTES[dayOfYear % QUOTES.size]
  }
}
