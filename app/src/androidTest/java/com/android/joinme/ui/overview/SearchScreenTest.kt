package com.android.joinme.ui.overview

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun setupScreen(viewModel: SearchViewModel = SearchViewModel()) {
    composeTestRule.setContent { SearchScreen(searchViewModel = viewModel, onGoBack = {}) }
  }

  @Test
  fun searchScreen_displaysTopBar() {
    setupScreen()

    composeTestRule.onNodeWithText("Search").assertIsDisplayed()
  }

  @Test
  fun searchScreen_displaysBackButton() {
    setupScreen()

    composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
  }

  @Test
  fun searchScreen_displaysSearchTextField() {
    setupScreen()

    composeTestRule.onNodeWithText("Search an event").assertIsDisplayed()
  }

  @Test
  fun searchScreen_searchTextFieldHasSearchIcon() {
    setupScreen()

    composeTestRule.onNodeWithContentDescription("Search").assertIsDisplayed()
  }

  @Test
  fun searchScreen_displaysAllFilterChip() {
    setupScreen()

    composeTestRule.onNodeWithText("All").assertIsDisplayed()
  }

  @Test
  fun searchScreen_displaysBarFilterChip() {
    setupScreen()

    composeTestRule.onNodeWithText("Bar").assertIsDisplayed()
  }

  @Test
  fun searchScreen_displaysClubFilterChip() {
    setupScreen()

    composeTestRule.onNodeWithText("Club").assertIsDisplayed()
  }

  @Test
  fun searchScreen_displaysSportFilterChip() {
    setupScreen()

    composeTestRule.onNodeWithText("Sport").assertIsDisplayed()
  }

  @Test
  fun searchScreen_sportFilterHasDropdownIcon() {
    setupScreen()

    composeTestRule.onNodeWithContentDescription("Dropdown").assertIsDisplayed()
  }

  @Test
  fun searchScreen_canEnterSearchQuery() {
    setupScreen()

    val searchQuery = "basketball"
    composeTestRule.onNodeWithText("Search an event").performTextInput(searchQuery)

    composeTestRule.onNodeWithText(searchQuery).assertIsDisplayed()
  }

  @Test
  fun searchScreen_backButtonIsClickable() {
    var backClicked = false
    composeTestRule.setContent {
      SearchScreen(searchViewModel = SearchViewModel(), onGoBack = { backClicked = true })
    }

    composeTestRule.onNodeWithContentDescription("Back").performClick()

    assert(backClicked)
  }

  @Test
  fun searchScreen_allFilterChipToggles() {
    setupScreen()

    val allChip = composeTestRule.onNodeWithText("All")

    // Initial state - not selected
    allChip.assertIsDisplayed()

    // Click to select
    allChip.performClick()

    // Click again to deselect
    allChip.performClick()

    allChip.assertIsDisplayed()
  }

  @Test
  fun searchScreen_barFilterChipToggles() {
    setupScreen()

    val barChip = composeTestRule.onNodeWithText("Bar")

    barChip.assertIsDisplayed()
    barChip.performClick()
    barChip.performClick()
    barChip.assertIsDisplayed()
  }

  @Test
  fun searchScreen_clubFilterChipToggles() {
    setupScreen()

    val clubChip = composeTestRule.onNodeWithText("Club")

    clubChip.assertIsDisplayed()
    clubChip.performClick()
    clubChip.performClick()
    clubChip.assertIsDisplayed()
  }

  @Test
  fun searchScreen_sportDropdownOpensOnClick() {
    setupScreen()

    // Initially, sport items are not displayed
    composeTestRule.onNodeWithText("Basket").assertDoesNotExist()

    // Click the Sport filter to open dropdown
    composeTestRule.onNodeWithText("Sport").performClick()

    // Now sport items should be visible
    composeTestRule.onNodeWithText("Select all").assertIsDisplayed()

    composeTestRule.onNodeWithText("Basket").assertIsDisplayed()
  }

  @Test
  fun searchScreen_sportDropdownShowsAllSports() {
    setupScreen()

    // Open dropdown
    composeTestRule.onNodeWithText("Sport").performClick()

    // Verify all sports are displayed
    composeTestRule.onNodeWithText("Select all").assertIsDisplayed()
    composeTestRule.onNodeWithText("Basket").assertIsDisplayed()
    composeTestRule.onNodeWithText("Football").assertIsDisplayed()
    composeTestRule.onNodeWithText("Tennis").assertIsDisplayed()
    composeTestRule.onNodeWithText("Running").assertIsDisplayed()
  }

  @Test
  fun searchScreen_canSelectSportFromDropdown() {
    setupScreen()

    // Open dropdown
    composeTestRule.onNodeWithText("Sport").performClick()

    // Click on Basket
    composeTestRule.onNodeWithText("Basket").performClick()

    // Verify it's still in the dropdown (for verification)
    composeTestRule.onNodeWithText("Basket").assertIsDisplayed()
  }

  @Test
  fun searchScreen_selectAllInDropdownWorks() {
    setupScreen()

    // Open dropdown
    composeTestRule.onNodeWithText("Sport").performClick()

    // Click Select all
    composeTestRule.onNodeWithText("Select all").performClick()

    // Verify dropdown still shows items
    composeTestRule.onNodeWithText("Basket").assertIsDisplayed()
  }

  @Test
  fun searchScreen_multipleFiltersCanBeSelected() {
    setupScreen()

    // Select All filter
    composeTestRule.onNodeWithText("All").performClick()

    // Select Bar filter
    composeTestRule.onNodeWithText("Bar").performClick()

    // Both should still be displayed
    composeTestRule.onNodeWithText("All").assertIsDisplayed()
    composeTestRule.onNodeWithText("Bar").assertIsDisplayed()
  }

  @Test
  fun searchScreen_canToggleMultipleSportsInDropdown() {
    setupScreen()

    // Open dropdown
    composeTestRule.onNodeWithText("Sport").performClick()

    // Select multiple sports
    composeTestRule.onNodeWithText("Basket").performClick()
    composeTestRule.onNodeWithText("Football").performClick()
    composeTestRule.onNodeWithText("Tennis").performClick()

    // All should still be visible
    composeTestRule.onNodeWithText("Basket").assertIsDisplayed()
    composeTestRule.onNodeWithText("Football").assertIsDisplayed()
    composeTestRule.onNodeWithText("Tennis").assertIsDisplayed()
  }

  @Test
  fun searchScreen_searchFieldClearsText() {
    setupScreen()

    composeTestRule.onNodeWithText("Search an event").performTextInput("test query")

    // Verify text was entered
    composeTestRule.onNodeWithText("test query").assertExists()

    // Clear button should be visible
    composeTestRule.onNodeWithContentDescription("Clear").assertIsDisplayed()

    // Click clear button
    composeTestRule.onNodeWithContentDescription("Clear").performClick()

    composeTestRule.waitForIdle()

    // Text should be cleared
    composeTestRule.onNodeWithText("test query").assertDoesNotExist()
  }

  @Test
  fun searchScreen_longSearchQueryIsAccepted() {
    setupScreen()

    val longQuery = "a".repeat(100)
    composeTestRule.onNodeWithText("Search an event").performTextInput(longQuery)

    composeTestRule.onNodeWithText(longQuery).assertIsDisplayed()
  }

  @Test
  fun searchScreen_allFiltersDisplayedSimultaneously() {
    setupScreen()

    // All filter chips should be visible at the same time
    composeTestRule.onNodeWithText("All").assertIsDisplayed()
    composeTestRule.onNodeWithText("Bar").assertIsDisplayed()
    composeTestRule.onNodeWithText("Club").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sport").assertIsDisplayed()
  }

  @Test
  fun searchScreen_dropdownClosesAndReopens() {
    setupScreen()

    // Open dropdown
    composeTestRule.onNodeWithText("Sport").performClick()

    composeTestRule.onNodeWithText("Basket").assertIsDisplayed()

    // Close by clicking outside (click on the screen)
    composeTestRule.onNodeWithText("Search").performClick()

    // Reopen
    composeTestRule.onNodeWithText("Sport").performClick()

    composeTestRule.onNodeWithText("Basket").assertIsDisplayed()
  }

  @Test
  fun searchScreen_viewModelIntegration_queryUpdates() {
    val viewModel = SearchViewModel()
    setupScreen(viewModel)

    composeTestRule.onNodeWithText("Search an event").performTextInput("test")

    composeTestRule.waitForIdle()

    assert(viewModel.uiState.value.query == "test")
  }

  @Test
  fun searchScreen_viewModelIntegration_allFilterUpdates() {
    val viewModel = SearchViewModel()
    setupScreen(viewModel)

    composeTestRule.onNodeWithText("All").performClick()

    composeTestRule.waitForIdle()

    assert(viewModel.uiState.value.isAllSelected)
  }

  @Test
  fun searchScreen_viewModelIntegration_barFilterUpdates() {
    val viewModel = SearchViewModel()
    setupScreen(viewModel)

    composeTestRule.onNodeWithText("Bar").performClick()

    composeTestRule.waitForIdle()

    assert(viewModel.uiState.value.isBarSelected)
  }

  @Test
  fun searchScreen_viewModelIntegration_clubFilterUpdates() {
    val viewModel = SearchViewModel()
    setupScreen(viewModel)

    composeTestRule.onNodeWithText("Club").performClick()

    composeTestRule.waitForIdle()

    assert(viewModel.uiState.value.isClubSelected)
  }

  @Test
  fun searchScreen_viewModelIntegration_sportSelectionUpdates() {
    val viewModel = SearchViewModel()
    setupScreen(viewModel)

    // Open dropdown
    composeTestRule.onNodeWithText("Sport").performClick()

    // Select a sport
    composeTestRule.onNodeWithText("Basket").performClick()

    composeTestRule.waitForIdle()

    val basketSport = viewModel.uiState.value.sportCategories.find { it.id == "basket" }
    assert(basketSport?.isChecked == true)
  }
}
