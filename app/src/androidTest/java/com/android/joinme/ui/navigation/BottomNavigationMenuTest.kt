package com.android.joinme.ui.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class BottomNavigationMenuTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun bottomNavigationMenu_displaysAllTabs() {
    composeTestRule.setContent {
      BottomNavigationMenu(
          selectedTab = Tab.Overview,
          onTabSelected = {},
      )
    }

    // Verify the root menu is present
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertExists()

    // Verify each tab is rendered
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Overview")).assertExists()
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Search")).assertExists()
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Map")).assertExists()
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Profile")).assertExists()
  }

  @Test
  fun selectedTab_isMarkedAsSelected() {
    composeTestRule.setContent {
      BottomNavigationMenu(
          selectedTab = Tab.Search,
          onTabSelected = {},
      )
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Search")).assertIsSelected()
  }

  @Test
  fun clickingTab_triggersOnTabSelected() {
    var selected: Tab? = null

    composeTestRule.setContent {
      BottomNavigationMenu(
          selectedTab = Tab.Overview,
          onTabSelected = { selected = it },
      )
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Map")).performClick()

    assert(selected == Tab.Map)
  }
}
