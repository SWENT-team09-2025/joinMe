package com.android.joinme.ui.navigation

object NavigationTestTags {
  // General navigation elements
  const val BOTTOM_NAVIGATION_MENU = "navigation_bottom_menu"
  const val GO_BACK_BUTTON = "navigation_go_back_button"
  const val TOP_BAR_TITLE = "navigation_top_bar_title"

  // Tabs
  fun tabTag(name: String) = "tab_${name.lowercase()}"
}
