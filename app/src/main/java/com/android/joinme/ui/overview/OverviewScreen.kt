package com.android.joinme.ui.overview

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.event.Event
import com.android.joinme.model.eventItem.EventItem
import com.android.joinme.model.serie.Serie
import com.android.joinme.ui.components.EventCard
import com.android.joinme.ui.components.SerieCard
import com.android.joinme.ui.navigation.BottomNavigationMenu
import com.android.joinme.ui.navigation.NavigationActions
import com.android.joinme.ui.navigation.NavigationTestTags
import com.android.joinme.ui.navigation.Tab
import com.android.joinme.ui.theme.DividerColor
import com.android.joinme.ui.theme.IconColor
import com.android.joinme.ui.theme.OverviewScreenButtonColor

/**
 * Test tags for UI testing of the Overview screen components.
 *
 * Provides consistent identifiers for testing individual UI elements including buttons, lists,
 * titles, and loading indicators.
 */
object OverviewScreenTestTags {
  const val CREATE_EVENT_BUTTON = "createEventFab"
  const val HISTORY_BUTTON = "historyButton"
  const val EMPTY_EVENT_LIST_MSG = "emptyEventList"
  const val EVENT_LIST = "eventList"
  const val ONGOING_EVENTS_TITLE = "ongoingEventsTitle"
  const val UPCOMING_EVENTS_TITLE = "upcomingEventsTitle"
  const val LOADING_INDICATOR = "overviewLoadingIndicator"

  /**
   * Generates a unique test tag for a specific event item.
   *
   * @param event The event to generate a tag for
   * @return A string combining "eventItem" with the event's unique ID
   */
  fun getTestTagForEvent(event: Event): String = "eventItem${event.eventId}"

  /**
   * Generates a unique test tag for a specific serie item.
   *
   * @param serie The serie to generate a tag for
   * @return A string combining "eventItem" with the serie's unique ID
   */
  fun getTestTagForSerie(serie: Serie): String = "serieItem${serie.serieId}"
}

/**
 * Main overview screen displaying both ongoing and upcoming activities.
 *
 * This screen shows a unified view of standalone events and event series, categorized into:
 * - **Ongoing activities**: Events/series currently in progress
 * - **Upcoming activities**: Events/series scheduled for the future
 *
 * Events that belong to a series are filtered out from the standalone event display to avoid
 * duplication.
 *
 * **UI States:**
 * - Loading: Displays a centered progress indicator
 * - Empty: Shows a message prompting users to join or create events
 * - Content: Displays ongoing and upcoming activities in separate sections
 *
 * **Features:**
 * - Automatic data refresh on screen launch
 * - Error handling with toast notifications
 * - Two FABs: Create event (bottom right) and View history (bottom left)
 * - Pattern matching to render EventCard or SerieCard based on item type
 *
 * @param overviewViewModel ViewModel managing the screen state and business logic
 * @param credentialManager Credential manager for authentication (currently unused)
 * @param onSelectEvent Callback invoked when a standalone event is clicked
 * @param onAddEvent Callback invoked when the create event FAB is clicked
 * @param onGoToHistory Callback invoked when the history FAB is clicked
 * @param navigationActions Navigation controller for bottom navigation menu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    overviewViewModel: OverviewViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onSelectEvent: (Event) -> Unit = {},
    onAddEvent: () -> Unit = {},
    onGoToHistory: () -> Unit = {},
    navigationActions: NavigationActions? = null,
) {

  val context = LocalContext.current
  val uiState by overviewViewModel.uiState.collectAsState()
  val ongoingItems = uiState.ongoingItems
  val upcomingItems = uiState.upcomingItems
  val isLoading = uiState.isLoading

  // Trigger data refresh when screen is first displayed
  LaunchedEffect(Unit) { overviewViewModel.refreshUIState() }

  // Display error messages as toasts and clear them from state
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      overviewViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = {
        Column {
          CenterAlignedTopAppBar(
              modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE),
              title = { Text(text = "Overview", style = MaterialTheme.typography.titleLarge) },
              colors =
                  TopAppBarDefaults.topAppBarColors(
                      containerColor = MaterialTheme.colorScheme.surface))
          HorizontalDivider(color = DividerColor, thickness = 1.dp)
        }
      },
      bottomBar = {
        BottomNavigationMenu(
            selectedTab = Tab.Overview,
            onTabSelected = { tab -> navigationActions?.navigateTo(tab.destination) },
            modifier = Modifier)
      },
      floatingActionButton = {
        FloatingActionButton(
            onClick = onAddEvent,
            containerColor = OverviewScreenButtonColor,
            modifier = Modifier.testTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON)) {
              Icon(Icons.Default.Add, contentDescription = "Add Event", tint = IconColor)
            }
      }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          when {
            isLoading -> {
              // Loading state
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag(OverviewScreenTestTags.LOADING_INDICATOR))
              }
            }
            ongoingItems.isEmpty() && upcomingItems.isEmpty() -> {
              // Empty state
              Column(
                  modifier = Modifier.fillMaxSize(),
                  verticalArrangement = Arrangement.Center,
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "You have no events yet. Join one, or create your own event.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag(OverviewScreenTestTags.EMPTY_EVENT_LIST_MSG))
                  }
            }
            else -> {
              // Content state: Display ongoing and upcoming activities
              LazyColumn(
                  contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                  modifier = Modifier.fillMaxWidth().testTag(OverviewScreenTestTags.EVENT_LIST)) {
                    // Ongoing activities section
                    if (ongoingItems.isNotEmpty()) {
                      item {
                        Text(
                            text =
                                if (ongoingItems.size == 1) "Your ongoing activity :"
                                else "Your ongoing activities :",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier =
                                Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                                    .testTag(OverviewScreenTestTags.ONGOING_EVENTS_TITLE))
                      }

                      // Render each ongoing item (event or serie)
                      items(ongoingItems.size) { index ->
                        when (val item = ongoingItems[index]) {
                          is EventItem.SingleEvent -> {
                            EventCard(
                                modifier = Modifier.padding(vertical = 6.dp),
                                event = item.event,
                                onClick = { onSelectEvent(item.event) },
                                testTag = OverviewScreenTestTags.getTestTagForEvent(item.event))
                          }
                          is EventItem.EventSerie -> {
                            SerieCard(
                                modifier = Modifier.padding(vertical = 6.dp),
                                serie = item.serie,
                                onClick = {
                                  Toast.makeText(context, "Not Implemented", Toast.LENGTH_SHORT)
                                      .show()
                                },
                                testTag = OverviewScreenTestTags.getTestTagForSerie(item.serie))
                          }
                        }
                      }

                      item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    // Upcoming activities section
                    if (upcomingItems.isNotEmpty()) {
                      item {
                        Text(
                            text =
                                if (upcomingItems.size == 1) "Your upcoming activity :"
                                else "Your upcoming activities :",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier =
                                Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                                    .testTag(OverviewScreenTestTags.UPCOMING_EVENTS_TITLE))
                      }

                      // Render each upcoming item (event or serie)
                      items(upcomingItems.size) { index ->
                        when (val item = upcomingItems[index]) {
                          is EventItem.SingleEvent -> {
                            EventCard(
                                modifier = Modifier.padding(vertical = 6.dp),
                                event = item.event,
                                onClick = { onSelectEvent(item.event) },
                                testTag = OverviewScreenTestTags.getTestTagForEvent(item.event))
                          }
                          is EventItem.EventSerie -> {
                            SerieCard(
                                modifier = Modifier.padding(vertical = 6.dp),
                                serie = item.serie,
                                onClick = {
                                  Toast.makeText(context, "Not Implemented", Toast.LENGTH_SHORT)
                                      .show()
                                },
                                testTag = OverviewScreenTestTags.getTestTagForSerie(item.serie))
                          }
                        }
                      }
                    }
                  }
            }
          }

          // FAB History on bottom left
          FloatingActionButton(
              onClick = onGoToHistory,
              containerColor = OverviewScreenButtonColor,
              modifier =
                  Modifier.align(Alignment.BottomStart)
                      .padding(start = 16.dp, bottom = 16.dp)
                      .testTag(OverviewScreenTestTags.HISTORY_BUTTON)) {
                Icon(Icons.Default.History, contentDescription = "View History", tint = IconColor)
              }
        }
      }
}
