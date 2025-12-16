package com.android.joinme.ui.groups

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.R
import com.android.joinme.model.event.Event
import com.android.joinme.model.eventItem.EventItem
import com.android.joinme.model.serie.Serie
import com.android.joinme.ui.components.EventCard
import com.android.joinme.ui.components.SerieCard
import com.android.joinme.ui.navigation.NavigationTestTags
import com.android.joinme.ui.theme.Dimens

/** Note: This file was co-written with the help of AI (Claude). */

/**
 * Test tags for UI testing of the ActivityGroup screen components.
 *
 * Provides consistent identifiers for testing individual UI elements including buttons, lists, and
 * loading indicators.
 */
object ActivityGroupScreenTestTags {
  const val BACK_BUTTON = "activityGroupBackButton"
  const val EMPTY_ACTIVITY_LIST_MSG = "emptyActivityList"
  const val ACTIVITY_LIST = "activityList"
  const val LOADING_INDICATOR = "activityGroupLoadingIndicator"

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
   * @return A string combining "serieItem" with the serie's unique ID
   */
  fun getTestTagForSerie(serie: Serie): String = "serieItem${serie.serieId}"
}

/**
 * Screen displaying all activities (events and series) for a specific group.
 *
 * This screen shows a unified view of all events and series associated with a group without
 * categorization into ongoing/upcoming sections.
 *
 * **UI States:**
 * - Loading: Displays a centered progress indicator
 * - Empty: Shows a message indicating the group has no activities
 * - Content: Displays all events and series in a single list
 * - Error: Shows error messages via toast notifications
 *
 * **Features:**
 * - Automatic data loading on screen launch
 * - Error handling with toast notifications
 * - Back navigation to previous screen
 * - Pattern matching to render EventCard or SerieCard
 *
 * @param groupId The unique identifier of the group whose activities are displayed
 * @param activityGroupViewModel ViewModel managing the screen state and business logic
 * @param onSelectedEvent Callback invoked when an event is clicked
 * @param onSelectedSerie Callback invoked when a serie is clicked
 * @param onNavigateBack Callback invoked when the back button is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityGroupScreen(
    groupId: String,
    activityGroupViewModel: ActivityGroupViewModel = viewModel(),
    onSelectedEvent: (String) -> Unit = {},
    onSelectedSerie: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
  val context = LocalContext.current
  val uiState by activityGroupViewModel.uiState.collectAsState()
  val items = uiState.items
  val groupName = uiState.groupName
  val isLoading = uiState.isLoading

  // Trigger data load when screen is first displayed
  LaunchedEffect(groupId) { activityGroupViewModel.load(groupId) }

  // Display error messages as toasts
  LaunchedEffect(uiState.error) {
    uiState.error?.let { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
  }

  Scaffold(
      topBar = {
        Column {
          CenterAlignedTopAppBar(
              modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE),
              title = {
                Text(
                    text =
                        if (groupName.isNotEmpty())
                            stringResource(R.string.group_activities_title, groupName)
                        else stringResource(R.string.group_activities_default),
                    style = MaterialTheme.typography.titleLarge)
              },
              navigationIcon = {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag(ActivityGroupScreenTestTags.BACK_BUTTON)) {
                      Icon(
                          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                          contentDescription = stringResource(R.string.back),
                          tint = MaterialTheme.colorScheme.onSurface)
                    }
              },
              colors =
                  TopAppBarDefaults.topAppBarColors(
                      containerColor = MaterialTheme.colorScheme.surface))
          HorizontalDivider(
              color = MaterialTheme.colorScheme.primary, thickness = Dimens.BorderWidth.thin)
        }
      }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          when {
            isLoading -> {
              // Loading state
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag(ActivityGroupScreenTestTags.LOADING_INDICATOR))
              }
            }
            items.isEmpty() -> {
              // Empty state
              Column(
                  modifier = Modifier.fillMaxSize(),
                  verticalArrangement = Arrangement.Center,
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.no_group_activities),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier =
                            Modifier.testTag(ActivityGroupScreenTestTags.EMPTY_ACTIVITY_LIST_MSG))
                  }
            }
            else -> {
              // Content state: Display all items (events and series mixed)
              LazyColumn(
                  contentPadding =
                      PaddingValues(
                          vertical = Dimens.Padding.small, horizontal = Dimens.Padding.medium),
                  modifier =
                      Modifier.fillMaxWidth().testTag(ActivityGroupScreenTestTags.ACTIVITY_LIST)) {
                    items(items.size) { index ->
                      when (val item = items[index]) {
                        is EventItem.SingleEvent -> {
                          EventCard(
                              modifier = Modifier.padding(vertical = Dimens.Padding.small),
                              event = item.event,
                              onClick = { onSelectedEvent(item.event.eventId) },
                              testTag = ActivityGroupScreenTestTags.getTestTagForEvent(item.event))
                        }
                        is EventItem.EventSerie -> {
                          SerieCard(
                              modifier = Modifier.padding(vertical = Dimens.Padding.small),
                              serie = item.serie,
                              onClick = { onSelectedSerie(item.serie.serieId) },
                              testTag = ActivityGroupScreenTestTags.getTestTagForSerie(item.serie))
                        }
                      }
                    }
                  }
            }
          }
        }
      }
}
