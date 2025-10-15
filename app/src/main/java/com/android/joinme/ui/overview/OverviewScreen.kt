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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.event.Event
import com.android.joinme.ui.components.EventCard
import com.android.joinme.ui.navigation.BottomNavigationMenu
import com.android.joinme.ui.navigation.NavigationActions
import com.android.joinme.ui.navigation.NavigationTestTags
import com.android.joinme.ui.navigation.Tab
import com.android.joinme.ui.theme.OverviewScreenButtonColor

object OverviewScreenTestTags {
  const val CREATE_EVENT_BUTTON = "createEventFab"
  const val HISTORY_BUTTON = "historyButton"
  const val EMPTY_EVENT_LIST_MSG = "emptyEventList"
  const val EVENT_LIST = "eventList"
  const val ONGOING_EVENTS_TITLE = "ongoingEventsTitle"
  const val UPCOMING_EVENTS_TITLE = "upcomingEventsTitle"
  const val LOADING_INDICATOR = "overviewLoadingIndicator"

  fun getTestTagForEventItem(event: Event): String = "eventItem${event.eventId}"
}

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
  val ongoingEvents = uiState.ongoingEvents
  val upcomingEvents = uiState.upcomingEvents
  val isLoading = uiState.isLoading

  LaunchedEffect(Unit) { overviewViewModel.refreshUIState() }

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      overviewViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = {
        Column {
          TopAppBar(
              modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE),
              title = {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center)
              },
              colors =
                  TopAppBarDefaults.topAppBarColors(
                      containerColor = MaterialTheme.colorScheme.surface))
          HorizontalDivider(color = Color.Black, thickness = 1.dp)
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
              Icon(Icons.Default.Add, contentDescription = "Add Event", tint = Color.Black)
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
            ongoingEvents.isEmpty() && upcomingEvents.isEmpty() -> {
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
              // Content state
              LazyColumn(
                  contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                  modifier = Modifier.fillMaxWidth().testTag(OverviewScreenTestTags.EVENT_LIST)) {
                    if (ongoingEvents.isNotEmpty()) {
                      item {
                        Text(
                            text =
                                if (ongoingEvents.size == 1) "Your ongoing event :"
                                else "Your ongoing events :",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier =
                                Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                                    .testTag(OverviewScreenTestTags.ONGOING_EVENTS_TITLE))
                      }

                      items(ongoingEvents.size) { index ->
                        EventCard(
                            modifier = Modifier.padding(vertical = 6.dp),
                            event = ongoingEvents[index],
                            onClick = { onSelectEvent(ongoingEvents[index]) },
                            testTag =
                                OverviewScreenTestTags.getTestTagForEventItem(ongoingEvents[index]))
                      }

                      item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    if (upcomingEvents.isNotEmpty()) {
                      item {
                        Text(
                            text =
                                if (upcomingEvents.size == 1) "Your upcoming event :"
                                else "Your upcoming events :",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier =
                                Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                                    .testTag(OverviewScreenTestTags.UPCOMING_EVENTS_TITLE))
                      }

                      items(upcomingEvents.size) { index ->
                        EventCard(
                            modifier = Modifier.padding(vertical = 6.dp),
                            event = upcomingEvents[index],
                            onClick = { onSelectEvent(upcomingEvents[index]) },
                            testTag =
                                OverviewScreenTestTags.getTestTagForEventItem(
                                    upcomingEvents[index]))
                      }
                    }
                  }
            }
          }

          // FAB History en bas à gauche
          FloatingActionButton(
              onClick = onGoToHistory,
              containerColor = OverviewScreenButtonColor,
              modifier =
                  Modifier.align(Alignment.BottomStart)
                      .padding(start = 16.dp, bottom = 16.dp)
                      .testTag(OverviewScreenTestTags.HISTORY_BUTTON)) {
                Icon(Icons.Default.History, contentDescription = "View History", tint = Color.Black)
              }
        }
      }
}
