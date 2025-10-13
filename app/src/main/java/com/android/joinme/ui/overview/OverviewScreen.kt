package com.android.joinme.ui.overview

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import com.android.joinme.model.event.EventType
import com.android.joinme.ui.navigation.BottomNavigationMenu
import com.android.joinme.ui.navigation.NavigationActions
import com.android.joinme.ui.navigation.NavigationTestTags
import com.android.joinme.ui.navigation.Tab
import java.text.SimpleDateFormat
import java.util.Locale

object OverviewScreenTestTags {
  const val CREATE_EVENT_BUTTON = "createEventFab"
  const val EMPTY_EVENT_LIST_MSG = "emptyEventList"
  const val EVENT_LIST = "eventList"
  // const val ONGOING_EVENTS_SECTION = "ongoingEventsSection"
  // const val UPCOMING_EVENTS_SECTION = "upcomingEventsSection"
  const val ONGOING_EVENTS_TITLE = "ongoingEventsTitle"
  const val UPCOMING_EVENTS_TITLE = "upcomingEventsTitle"

  fun getTestTagForEventItem(event: Event): String = "eventItem${event.eventId}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    overviewViewModel: OverviewViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onSelectEvent: (Event) -> Unit = {},
    onAddEvent: () -> Unit = {},
    navigationActions: NavigationActions? = null,
) {

  val context = LocalContext.current
  val uiState by overviewViewModel.uiState.collectAsState()
  val ongoingEvents = uiState.ongoingEvents
  val upcomingEvents = uiState.upcomingEvents

  // Fetch events when the screen is recomposed
  LaunchedEffect(Unit) { overviewViewModel.refreshUIState() }

  // Show error message if fetching events fails
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
                    text = "Welcome, Mathieu",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center)
              },
              colors =
                  TopAppBarDefaults.topAppBarColors(
                      containerColor = MaterialTheme.colorScheme.surface))
          Divider(color = Color.Black, thickness = 1.dp)
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
            containerColor = Color(0xFFEDE7F6),
            modifier = Modifier.testTag(OverviewScreenTestTags.CREATE_EVENT_BUTTON)) {
              Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
      }) { innerPadding ->
        if (ongoingEvents.isEmpty() && upcomingEvents.isEmpty()) {
          // Empty state
          Column(
              modifier = Modifier.fillMaxSize().padding(innerPadding),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "You have no events yet. Join one, or create your own event.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag(OverviewScreenTestTags.EMPTY_EVENT_LIST_MSG))
              }
        } else {
          // Events list with sections
          LazyColumn(
              contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(innerPadding)
                      .testTag(OverviewScreenTestTags.EVENT_LIST)) {

                // Ongoing Events Section
                if (ongoingEvents.isNotEmpty()) {
                  item {
                    Text(
                        text = "Your ongoing events :",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                                .testTag(OverviewScreenTestTags.ONGOING_EVENTS_TITLE))
                  }

                  items(ongoingEvents.size) { index ->
                    EventCard(
                        event = ongoingEvents[index],
                        onClick = { onSelectEvent(ongoingEvents[index]) })
                  }

                  item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Upcoming Events Section
                if (upcomingEvents.isNotEmpty()) {
                  item {
                    Text(
                        text = "Events to come :",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                                .testTag(OverviewScreenTestTags.UPCOMING_EVENTS_TITLE))
                  }

                  items(upcomingEvents.size) { index ->
                    EventCard(
                        event = upcomingEvents[index],
                        onClick = { onSelectEvent(upcomingEvents[index]) })
                  }
                }
              }
        }
      }
}

@Composable
fun EventCard(event: Event, onClick: () -> Unit) {
  val backgroundColor =
      when (event.type) {
        EventType.SPORTS -> Color(0xFF7E57C2) // Violet
        EventType.ACTIVITY -> Color(0xFF81C784) // Vert
        EventType.SOCIAL -> Color(0xFFE57373) // Rouge
      }

  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 6.dp, horizontal = 8.dp)
              .testTag(OverviewScreenTestTags.getTestTagForEventItem(event))
              .clickable(onClick = onClick),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = backgroundColor),
      elevation = CardDefaults.cardElevation(6.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
          // Date + Heure Row
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text =
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(event.date.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White)

                Text(
                    text =
                        SimpleDateFormat("HH'h'mm", Locale.getDefault())
                            .format(event.date.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White)
              }

          Spacer(modifier = Modifier.height(6.dp))

          // Titre
          Text(
              text = event.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White)

          Spacer(modifier = Modifier.height(4.dp))

          // Lieu + flèche
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Place : ${event.location?.name ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White)
              }
        }
      }
}
