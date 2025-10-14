package com.android.joinme.ui.history

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.event.Event
import com.android.joinme.ui.components.EventCard

object HistoryScreenTestTags {
  const val SCREEN = "historyScreen"
  const val TOP_BAR = "historyTopBar"
  const val BACK_BUTTON = "historyBackButton"
  const val EMPTY_HISTORY_MSG = "emptyHistoryMessage"
  const val HISTORY_LIST = "historyList"
  const val LOADING_INDICATOR = "historyLoadingIndicator"

  fun getTestTagForEventItem(event: Event): String = "historyEventItem${event.eventId}"
}

/**
 * Screen displaying a list of expired events from the user's history.
 *
 * Shows all events that have passed their end time (date + duration), sorted by date in descending
 * order (most recent first). Users can tap on events to view details or navigate back to the
 * previous screen.
 *
 * @param historyViewModel ViewModel managing the history state and expired events data.
 * @param onSelectEvent Callback invoked when a user taps on an event card.
 * @param onGoBack Callback invoked when the user taps the back button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel = viewModel(),
    onSelectEvent: (Event) -> Unit = {},
    onGoBack: () -> Unit = {},
) {
  val context = LocalContext.current
  val uiState by historyViewModel.uiState.collectAsState()
  val expiredEvents = uiState.expiredEvents
  val isLoading = uiState.isLoading

  LaunchedEffect(Unit) { historyViewModel.refreshUIState() }

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      historyViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = Modifier.testTag(HistoryScreenTestTags.SCREEN),
      topBar = {
        Column {
          TopAppBar(
              modifier = Modifier.testTag(HistoryScreenTestTags.TOP_BAR),
              title = {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center)
              },
              navigationIcon = {
                IconButton(
                    onClick = onGoBack,
                    modifier = Modifier.testTag(HistoryScreenTestTags.BACK_BUTTON)) {
                      Icon(
                          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                          contentDescription = "Go back")
                    }
              },
              actions = {
                // Empty Box to balance the navigation icon and center the title
                Box(modifier = Modifier.size(48.dp))
              },
              colors =
                  TopAppBarDefaults.topAppBarColors(
                      containerColor = MaterialTheme.colorScheme.surface))
          HorizontalDivider(color = Color.Black, thickness = 1.dp)
        }
      }) { innerPadding ->
        when {
          isLoading -> {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(
                      modifier = Modifier.testTag(HistoryScreenTestTags.LOADING_INDICATOR))
                }
          }
          expiredEvents.isEmpty() -> {
            // Empty state
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                      text =
                          "You have nothing in your history yet. Participate at an event to see it here!",
                      textAlign = TextAlign.Center,
                      style = MaterialTheme.typography.bodyMedium,
                      modifier =
                          Modifier.padding(horizontal = 32.dp)
                              .testTag(HistoryScreenTestTags.EMPTY_HISTORY_MSG))
                }
          }
          else -> {
            // Content state
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(innerPadding)
                        .testTag(HistoryScreenTestTags.HISTORY_LIST)) {
                  items(expiredEvents.size) { index ->
                    EventCard(
                        modifier = Modifier.padding(vertical = 6.dp),
                        event = expiredEvents[index],
                        onClick = { onSelectEvent(expiredEvents[index]) },
                        testTag =
                            HistoryScreenTestTags.getTestTagForEventItem(expiredEvents[index]))
                  }
                }
          }
        }
      }
}
