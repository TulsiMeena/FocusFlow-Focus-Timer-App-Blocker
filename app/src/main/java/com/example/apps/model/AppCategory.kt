package com.example.apps.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppCategory(
  val displayName: String,
  val icon: ImageVector
) {
  ALL("All", Icons.Default.Apps),
  SOCIAL("Social", Icons.Default.Chat),
  ENTERTAINMENT("Video & Audio", Icons.Default.Movie),
  GAMES("Games", Icons.Default.SportsEsports),
  BROWSING_SHOPPING("Browsing & Shop", Icons.Default.Language),
  OTHER("Other", Icons.Default.Apps);

  companion object {
    fun fromPackageAndName(packageName: String, appName: String): AppCategory {
      val lowerPkg = packageName.lowercase()
      val lowerName = appName.lowercase()
      return when {
        lowerPkg.contains("whatsapp") || lowerPkg.contains("instagram") || lowerPkg.contains("facebook") ||
          lowerPkg.contains("tiktok") || lowerPkg.contains("twitter") || lowerPkg.contains(".katana") ||
          lowerPkg.contains("snapchat") || lowerPkg.contains("reddit") || lowerPkg.contains("linkedin") ||
          lowerPkg.contains("telegram") || lowerPkg.contains("discord") || lowerPkg.contains("threads") ||
          lowerName.contains("chat") || lowerName.contains("social") -> SOCIAL

        lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || lowerPkg.contains("primevideo") ||
          lowerPkg.contains("disney") || lowerPkg.contains("twitch") || lowerPkg.contains("spotify") ||
          lowerPkg.contains("hotstar") || lowerPkg.contains("hulu") || lowerPkg.contains("music") ||
          lowerPkg.contains("videoplayer") -> ENTERTAINMENT

        lowerPkg.contains("game") || lowerPkg.contains("supercell") || lowerPkg.contains("pubg") ||
          lowerPkg.contains("freefire") || lowerPkg.contains("roblox") || lowerPkg.contains("candy") ||
          lowerPkg.contains("riot") || lowerPkg.contains("epicgames") || lowerPkg.contains("arcade") -> GAMES

        lowerPkg.contains("amazon") || lowerPkg.contains("flipkart") || lowerPkg.contains("chrome") ||
          lowerPkg.contains("browser") || lowerPkg.contains("firefox") || lowerPkg.contains("opera") ||
          lowerPkg.contains("shopping") || lowerPkg.contains("ebay") || lowerPkg.contains("myntra") ||
          lowerPkg.contains("meesho") -> BROWSING_SHOPPING

        else -> OTHER
      }
    }
  }
}
