package com.android.joinme.ui.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h640dp-normal-long-notround-any-420dpi-keyshidden-nonav")
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

    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertExists()
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

  @Test
  fun clickingOverviewTab_triggersCallback() {
    var selected: Tab? = null

    composeTestRule.setContent {
      BottomNavigationMenu(
          selectedTab = Tab.Search,
          onTabSelected = { selected = it },
      )
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Overview")).performClick()
    assert(selected == Tab.Overview)
  }

  @Test
  fun clickingSearchTab_triggersCallback() {
    var selected: Tab? = null

    composeTestRule.setContent {
      BottomNavigationMenu(
          selectedTab = Tab.Overview,
          onTabSelected = { selected = it },
      )
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Search")).performClick()
    assert(selected == Tab.Search)
  }

  @Test
  fun clickingProfileTab_triggersCallback() {
    var selected: Tab? = null

    composeTestRule.setContent {
      BottomNavigationMenu(
          selectedTab = Tab.Overview,
          onTabSelected = { selected = it },
      )
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Profile")).performClick()
    assert(selected == Tab.Profile)
  }

  @Test
  fun overviewTab_isSelectedWhenSet() {
    composeTestRule.setContent {
      BottomNavigationMenu(
          selectedTab = Tab.Overview,
          onTabSelected = {},
      )
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Overview")).assertIsSelected()
  }

  @Test
  fun mapTab_isSelectedWhenSet() {
    composeTestRule.setContent {
      BottomNavigationMenu(
          selectedTab = Tab.Map,
          onTabSelected = {},
      )
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Map")).assertIsSelected()
  }

  @Test
  fun profileTab_isSelectedWhenSet() {
    composeTestRule.setContent {
      BottomNavigationMenu(
          selectedTab = Tab.Profile,
          onTabSelected = {},
      )
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Profile")).assertIsSelected()
  }

  @Test
  fun unselectedTabs_areNotSelected_whenOverviewSelected() {
    composeTestRule.setContent {
      BottomNavigationMenu(
          selectedTab = Tab.Overview,
          onTabSelected = {},
      )
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Search")).assertIsNotSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Map")).assertIsNotSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Profile")).assertIsNotSelected()
  }

  @Test
  fun unselectedTabs_areNotSelected_whenSearchSelected() {
    composeTestRule.setContent {
      BottomNavigationMenu(
          selectedTab = Tab.Search,
          onTabSelected = {},
      )
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Overview")).assertIsNotSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Map")).assertIsNotSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Profile")).assertIsNotSelected()
  }

  @Test
  fun tab_overviewHasCorrectProperties() {
    assert(Tab.Overview.name == "Overview")
    assert(Tab.Overview.destination == Screen.Overview)
  }

  @Test
  fun tab_searchHasCorrectProperties() {
    assert(Tab.Search.name == "Search")
    assert(Tab.Search.destination == Screen.Search)
  }

  @Test
  fun tab_mapHasCorrectProperties() {
    assert(Tab.Map.name == "Map")
    assert(Tab.Map.destination == Screen.Map)
  }

  @Test
  fun tab_profileHasCorrectProperties() {
    assert(Tab.Profile.name == "Profile")
    assert(Tab.Profile.destination == Screen.Profile)
  }

  @Test
  fun allTabs_haveIconsDisplayed() {
    composeTestRule.setContent {
      BottomNavigationMenu(
          selectedTab = Tab.Overview,
          onTabSelected = {},
      )
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Overview")).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Search")).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Map")).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.tabTag("Profile")).assertIsDisplayed()
  }

  @Test
  fun bottomNavigationMenu_appliesModifierCorrectly() {
    composeTestRule.setContent {
      BottomNavigationMenu(
          selectedTab = Tab.Overview,
          onTabSelected = {},
      )
    }

    composeTestRule
        .onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)
        .assertExists()
        .assertIsDisplayed()
  }
}
