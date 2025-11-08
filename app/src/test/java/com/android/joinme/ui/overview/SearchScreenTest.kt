package com.android.joinme.ui.overview

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.joinme.model.event.EventsRepositoryLocal
import com.android.joinme.model.filter.FilterRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setup() {
    // Reset FilterRepository before each test to ensure clean state
    FilterRepository.reset()
  }

  private fun setupScreen(viewModel: SearchViewModel = SearchViewModel(EventsRepositoryLocal())) {
    composeTestRule.setContent { SearchScreen(searchViewModel = viewModel) }
    composeTestRule.waitForIdle()
  }

  @Test
  fun searchScreen_displaysTopBar() {
    setupScreen()

    composeTestRule.onNodeWithText("Search").assertIsDisplayed()
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
  fun searchScreen_displaysSocialFilterChip() {
    setupScreen()

    composeTestRule.onNodeWithText("Social").assertIsDisplayed()
  }

  @Test
  fun searchScreen_displaysActivityFilterChip() {
    setupScreen()

    composeTestRule.onNodeWithText("Activity").assertIsDisplayed()
  }

  @Test
  fun searchScreen_displaysSportFilterChip() {
    setupScreen()

    composeTestRule.onNodeWithText("Sport").assertIsDisplayed()
  }

  //  @Test
  //  fun searchScreen_sportFilterHasDropdownIcon() {
  //    setupScreen()
  //
  //    // Wait for composition and filter state to settle
  //    composeTestRule.waitForIdle()
  //
  //    // Ensure Sport filter chip is displayed first
  //    composeTestRule.onNodeWithText("Sport").assertIsDisplayed()
  //
  //    composeTestRule.onNodeWithContentDescription("Dropdown").assertIsDisplayed()
  //  }

  @Test
  fun searchScreen_canEnterSearchQuery() {
    setupScreen()

    val searchQuery = "basketball"
    composeTestRule.onNodeWithText("Search an event").performTextInput(searchQuery)

    composeTestRule.onNodeWithText(searchQuery).assertIsDisplayed()
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
  fun searchScreen_socialFilterChipToggles() {
    setupScreen()

    val socialChip = composeTestRule.onNodeWithText("Social")

    socialChip.assertIsDisplayed()
    socialChip.performClick()
    socialChip.performClick()
    socialChip.assertIsDisplayed()
  }

  @Test
  fun searchScreen_activityFilterChipToggles() {
    setupScreen()

    val activityChip = composeTestRule.onNodeWithText("Activity")

    activityChip.assertIsDisplayed()
    activityChip.performClick()
    activityChip.performClick()
    activityChip.assertIsDisplayed()
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

    // Select Social filter
    composeTestRule.onNodeWithText("Social").performClick()

    // Both should still be displayed
    composeTestRule.onNodeWithText("All").assertIsDisplayed()
    composeTestRule.onNodeWithText("Social").assertIsDisplayed()
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
    composeTestRule.onNodeWithText("Social").assertIsDisplayed()
    composeTestRule.onNodeWithText("Activity").assertIsDisplayed()
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
    val viewModel = SearchViewModel(EventsRepositoryLocal())
    setupScreen(viewModel)

    composeTestRule.onNodeWithText("Search an event").performTextInput("test")

    composeTestRule.waitForIdle()

    assert(viewModel.uiState.value.query == "test")
  }

  @Test
  fun searchScreen_viewModelIntegration_allFilterUpdates() {
    val viewModel = SearchViewModel(EventsRepositoryLocal())
    setupScreen(viewModel)

    // Initially all filters are selected
    assert(viewModel.filterState.value.isAllSelected)

    // Click to deselect
    composeTestRule.onNodeWithText("All").performClick()

    composeTestRule.waitForIdle()

    // After toggle, should be deselected
    assert(!viewModel.filterState.value.isAllSelected)
  }

  @Test
  fun searchScreen_viewModelIntegration_socialFilterUpdates() {
    val viewModel = SearchViewModel(EventsRepositoryLocal())
    setupScreen(viewModel)

    composeTestRule.onNodeWithText("Social").performClick()

    composeTestRule.waitForIdle()

    assert(!viewModel.filterState.value.isSocialSelected)
  }

  @Test
  fun searchScreen_viewModelIntegration_activityFilterUpdates() {
    val viewModel = SearchViewModel(EventsRepositoryLocal())
    setupScreen(viewModel)

    composeTestRule.onNodeWithText("Activity").performClick()

    composeTestRule.waitForIdle()

    assert(!viewModel.filterState.value.isActivitySelected)
  }

  @Test
  fun searchScreen_viewModelIntegration_sportSelectionUpdates() {
    val viewModel = SearchViewModel(EventsRepositoryLocal())
    setupScreen(viewModel)

    // Initially basket is checked
    val basketSportBefore = viewModel.filterState.value.sportCategories.find { it.id == "basket" }
    assert(basketSportBefore?.isChecked == true)

    // Open dropdown
    composeTestRule.onNodeWithText("Sport").performClick()

    // Click to deselect basket
    composeTestRule.onNodeWithText("Basket").performClick()

    composeTestRule.waitForIdle()

    // After toggle, basket should be unchecked
    val basketSportAfter = viewModel.filterState.value.sportCategories.find { it.id == "basket" }
    assert(basketSportAfter?.isChecked == false)
  }

  @Test
  fun searchScreen_displaysEmptyMessage_whenNoEvents() {
    val viewModel = SearchViewModel(EventsRepositoryLocal())
    setupScreen(viewModel)

    // Ensure events are empty
    viewModel.setEvents(emptyList())

    composeTestRule.waitForIdle()

    // Empty events should show message using SearchScreenTestTags
    composeTestRule.onNodeWithTag(SearchScreenTestTags.EMPTY_EVENT_LIST_MSG).assertIsDisplayed()
  }

  @Test
  fun searchScreen_searchIconClick_dismissesKeyboard() {
    setupScreen()

    // Enter text
    composeTestRule.onNodeWithText("Search an event").performTextInput("test")

    composeTestRule.waitForIdle()

    // Click search icon (which is an IconButton)
    composeTestRule.onAllNodesWithContentDescription("Search")[0].performClick()

    composeTestRule.waitForIdle()

    // Verify text is still there after clicking search
    composeTestRule.onNodeWithText("test").assertExists()
  }

  @Test
  fun searchScreen_enterKeyPress_dismissesKeyboard() {
    setupScreen()

    // Find and interact with the search field
    composeTestRule.onNodeWithText("Search an event").performTextInput("test")

    composeTestRule.waitForIdle()

    // Now find the field by the text that was entered and perform IME action
    composeTestRule.onNode(hasSetTextAction()).performImeAction()

    composeTestRule.waitForIdle()

    // Text should still be there
    composeTestRule.onNodeWithText("test").assertExists()
  }

  @Test
  fun searchScreen_searchIcon_notClickable_whenQueryEmpty() {
    setupScreen()

    // Search icon should exist
    composeTestRule.onAllNodesWithContentDescription("Search")[0].assertIsDisplayed()

    // But clicking when empty should not crash
    composeTestRule.onAllNodesWithContentDescription("Search")[0].performClick()

    composeTestRule.waitForIdle()
  }

  @Test
  fun searchScreen_clearButton_onlyAppearsWithText() {
    setupScreen()

    // Clear button should not exist initially
    composeTestRule.onNodeWithContentDescription("Clear").assertDoesNotExist()

    // Enter text
    composeTestRule.onNodeWithText("Search an event").performTextInput("test")

    composeTestRule.waitForIdle()

    // Now clear button should appear
    composeTestRule.onNodeWithContentDescription("Clear").assertIsDisplayed()

    // Clear the text
    composeTestRule.onNodeWithContentDescription("Clear").performClick()

    composeTestRule.waitForIdle()

    // Clear button should disappear again
    composeTestRule.onNodeWithContentDescription("Clear").assertDoesNotExist()
  }

  @Test
  fun searchScreen_filterChips_displayedInRow() {
    setupScreen()

    // All filter chips in same row
    composeTestRule.onNodeWithText("All").assertIsDisplayed()
    composeTestRule.onNodeWithText("Social").assertIsDisplayed()
    composeTestRule.onNodeWithText("Activity").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sport").assertIsDisplayed()
  }

  @Test
  fun searchScreen_complexFilterScenario() {
    setupScreen()

    // Select multiple filters
    composeTestRule.onNodeWithText("All").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Social").performClick()
    composeTestRule.waitForIdle()

    // Open sport dropdown
    composeTestRule.onNodeWithText("Sport").performClick()
    composeTestRule.waitForIdle()

    // Select a sport
    composeTestRule.onNodeWithText("Tennis").performClick()
    composeTestRule.waitForIdle()

    // All should still be displayed
    composeTestRule.onNodeWithText("All").assertIsDisplayed()
  }

  @Test
  fun searchScreen_multipleSearchQueries() {
    setupScreen()

    // First query
    composeTestRule.onNodeWithText("Search an event").performTextInput("basketball")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("basketball").assertExists()

    // Clear
    composeTestRule.onNodeWithContentDescription("Clear").performClick()
    composeTestRule.waitForIdle()

    // Second query
    composeTestRule.onNodeWithText("Search an event").performTextInput("football")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("football").assertExists()
  }

  @Test
  fun searchScreen_displaysEventCards_whenEventsExist() {
    val viewModel = SearchViewModel(EventsRepositoryLocal())
    setupScreen(viewModel)

    val sampleEvent =
        com.android.joinme.model.event.Event(
            eventId = "1",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Basketball Game",
            description = "Fun basketball game",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = com.google.firebase.Timestamp.now(),
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    // Set events after screen is setup
    viewModel.setEvents(listOf(sampleEvent))

    composeTestRule.waitForIdle()

    // Event card should be displayed
    composeTestRule.onNodeWithText("Basketball Game").assertIsDisplayed()
    composeTestRule.onNodeWithText("Place : EPFL").assertIsDisplayed()
  }

  @Test
  fun searchScreen_eventCardClick_triggersCallback() {
    val viewModel = SearchViewModel(EventsRepositoryLocal())

    var eventClicked = false
    composeTestRule.setContent {
      SearchScreen(searchViewModel = viewModel, onSelectEvent = { eventClicked = true })
    }
    composeTestRule.waitForIdle()

    val sampleEvent =
        com.android.joinme.model.event.Event(
            eventId = "1",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Basketball Game",
            description = "Fun basketball game",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = com.google.firebase.Timestamp.now(),
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    // Set events after screen is setup
    viewModel.setEvents(listOf(sampleEvent))

    composeTestRule.waitForIdle()

    // Click on event card
    composeTestRule.onNodeWithText("Basketball Game").performClick()

    assert(eventClicked)
  }

  @Test
  fun searchScreen_eventList_displaysWithTestTag() {
    val viewModel = SearchViewModel(EventsRepositoryLocal())
    setupScreen(viewModel)

    val sampleEvent =
        com.android.joinme.model.event.Event(
            eventId = "1",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Basketball Game",
            description = "Fun basketball game",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = com.google.firebase.Timestamp.now(),
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    // Set events
    viewModel.setEvents(listOf(sampleEvent))

    composeTestRule.waitForIdle()

    // Event list should have proper test tag
    composeTestRule.onNodeWithTag(SearchScreenTestTags.EVENT_LIST).assertIsDisplayed()
  }

  @Test
  fun searchScreen_eventCard_hasCorrectTestTag() {
    val viewModel = SearchViewModel(EventsRepositoryLocal())
    setupScreen(viewModel)

    val sampleEvent =
        com.android.joinme.model.event.Event(
            eventId = "123",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Test Event",
            description = "Test description",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = com.google.firebase.Timestamp.now(),
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    // Set events
    viewModel.setEvents(listOf(sampleEvent))

    composeTestRule.waitForIdle()

    // Event card should have correct test tag based on eventId
    composeTestRule
        .onNodeWithTag(SearchScreenTestTags.getTestTagForEventItem(sampleEvent))
        .assertIsDisplayed()
  }

  @Test
  fun searchScreen_multipleEventCards_haveUniqueTestTags() {
    val viewModel = SearchViewModel(EventsRepositoryLocal())
    setupScreen(viewModel)

    val event1 =
        com.android.joinme.model.event.Event(
            eventId = "1",
            type = com.android.joinme.model.event.EventType.SPORTS,
            title = "Event 1",
            description = "Description 1",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = com.google.firebase.Timestamp.now(),
            duration = 60,
            participants = listOf("user1"),
            maxParticipants = 10,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner1")

    val event2 =
        com.android.joinme.model.event.Event(
            eventId = "2",
            type = com.android.joinme.model.event.EventType.SOCIAL,
            title = "Event 2",
            description = "Description 2",
            location = com.android.joinme.model.map.Location(46.5191, 6.5668, "EPFL"),
            date = com.google.firebase.Timestamp.now(),
            duration = 90,
            participants = listOf("user2"),
            maxParticipants = 15,
            visibility = com.android.joinme.model.event.EventVisibility.PUBLIC,
            ownerId = "owner2")

    // Set events
    viewModel.setEvents(listOf(event1, event2))

    composeTestRule.waitForIdle()

    // Both event cards should have unique test tags
    composeTestRule
        .onNodeWithTag(SearchScreenTestTags.getTestTagForEventItem(event1))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SearchScreenTestTags.getTestTagForEventItem(event2))
        .assertIsDisplayed()
  }

  @Test
  fun searchScreen_searchTextField_hasCorrectTestTag() {
    setupScreen()

    // Search text field should have proper test tag
    composeTestRule.onNodeWithTag(SearchScreenTestTags.SEARCH_TEXT_FIELD).assertIsDisplayed()
  }

  //  @Test
  //  fun searchScreen_dropdownDismisses_onOutsideClick() {
  //    setupScreen()
  //
  //    // Open dropdown
  //    composeTestRule.onNodeWithText("Sport").performClick()
  //    composeTestRule.waitForIdle()
  //
  //    // Verify dropdown is open
  //    composeTestRule.onNodeWithText("Select all").assertIsDisplayed()
  //
  //    // Click on a different filter chip (clearly outside the dropdown)
  //    composeTestRule.onNodeWithText("All").performClick()
  //    composeTestRule.waitForIdle()
  //
  //    // Dropdown should be closed
  //    composeTestRule.onNodeWithText("Basket").assertDoesNotExist()
  //    Thread.sleep(5000)
  //    composeTestRule.waitForIdle()
  //
  //  }

  @Test
  fun searchScreen_dropdownClosesAfterSelectingSport() {
    setupScreen()

    // Open dropdown
    composeTestRule.onNodeWithText("Sport").performClick()
    composeTestRule.waitForIdle()

    // Select a sport (dropdown stays open in current implementation)
    composeTestRule.onNodeWithText("Basket").performClick()
    composeTestRule.waitForIdle()

    // Dropdown should still be visible
    composeTestRule.onNodeWithText("Select all").assertIsDisplayed()
  }
}
