package com.android.joinme.ui.map

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapScreenTest {
  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          android.Manifest.permission.ACCESS_FINE_LOCATION,
          android.Manifest.permission.ACCESS_COARSE_LOCATION)

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun mapScreen_tagsAreDisplayed() {
    composeTestRule.setContent { MapScreen(viewModel = MapViewModel(), navigationActions = null) }

    composeTestRule
        .onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(MapScreenTestTags.MAP_CONTAINER)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(MapScreenTestTags.FILTER_BUTTON)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun mapScreen_filterButton_isClickable() {
    composeTestRule.setContent { MapScreen(viewModel = MapViewModel(), navigationActions = null) }

    composeTestRule
        .onNodeWithTag(MapScreenTestTags.FILTER_BUTTON)
        .assertExists()
        .assertHasClickAction()
        .performClick()
  }

  @Test
  fun mapScreen_filterIcon_hasCorrectLabel() {
    composeTestRule.setContent { MapScreen(viewModel = MapViewModel(), navigationActions = null) }
    composeTestRule.onNodeWithContentDescription("Filter").assertExists().assertIsDisplayed()
  }

  @Test
  fun mapScreen_bottomNavigationIsDisplayed() {
    composeTestRule.setContent { MapScreen(viewModel = MapViewModel(), navigationActions = null) }

    composeTestRule.onNodeWithTag("navigation_bottom_menu").assertExists()
  }
}
