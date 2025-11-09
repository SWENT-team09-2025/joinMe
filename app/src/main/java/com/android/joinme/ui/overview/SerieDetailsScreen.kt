package com.android.joinme.ui.overview

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.ui.components.EventCard
import com.android.joinme.ui.theme.onBackgroundLight
import com.android.joinme.ui.theme.onPrimaryLight
import com.android.joinme.ui.theme.outlineVariantLight
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

/**
 * Test tags for UI testing of the Serie Details screen components.
 *
 * Provides consistent identifiers for testing individual UI elements.
 */
object SerieDetailsScreenTestTags {
  /** Test tag for the screen root */
  const val SCREEN = "serieDetailsScreen"

  /** Test tag for the serie title in the top bar */
  const val SERIE_TITLE = "serieTitle"

  /** Test tag for the meeting date/time text */
  const val MEETING_INFO = "meetingInfo"

  /** Test tag for the visibility text (PUBLIC/PRIVATE) */
  const val VISIBILITY = "visibility"

  /** Test tag for the members count text */
  const val MEMBERS_COUNT = "membersCount"

  /** Test tag for the duration text */
  const val DURATION = "duration"

  /** Test tag for the description text */
  const val DESCRIPTION = "description"

  /** Test tag for the event list container */
  const val EVENT_LIST = "eventList"

  /** Test tag prefix for individual event cards */
  const val EVENT_CARD = "eventCard"

  /** Test tag for the owner info text */
  const val OWNER_INFO = "ownerInfo"

  /** Test tag for the "Add event" button */
  const val BUTTON_ADD_EVENT = "buttonAddEvent"

  /** Test tag for the "Quit serie" button */
  const val BUTTON_QUIT_SERIE = "buttonQuitSerie"

  /** Test tag for the loading indicator */
  const val LOADING = "loading"

  /** Test tag for the back button */
  const val BACK_BUTTON = "backButton"

  /** Test tag for the edit serie button */
  const val EDIT_SERIE_BUTTON = "editSerieButton"
}

/**
 * Screen for displaying the details of a serie.
 *
 * Shows serie information including title, date, visibility, participants count, duration,
 * description, and a list of associated events. Provides buttons for adding events (owner only) and
 * quitting the serie.
 *
 * @param serieId The unique identifier of the serie to display
 * @param serieDetailsViewModel ViewModel managing the screen state and business logic
 * @param onGoBack Callback invoked when the back button is pressed
 * @param onEventCardClick Callback invoked when an event card is clicked, receives the event ID
 * @param onAddEventClick Callback invoked when the "Add event" button is clicked
 * @param onQuitSerieSuccess Callback invoked when the user successfully quits the serie
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SerieDetailsScreen(
    serieId: String,
    serieDetailsViewModel: SerieDetailsViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onEventCardClick: (String) -> Unit = {},
    onAddEventClick: () -> Unit = {},
    onQuitSerieSuccess: () -> Unit = {},
    onEditSerieClick: () -> Unit = {},
    currentUserId: String = Firebase.auth.currentUser?.uid ?: "unknown"
) {
  val uiState by serieDetailsViewModel.uiState.collectAsState()
  val errorMsg = uiState.errorMsg
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  // Load serie details when the screen is first displayed
  LaunchedEffect(serieId) { serieDetailsViewModel.loadSerieDetails(serieId) }

  // Show error messages as toasts
  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      serieDetailsViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = Modifier.testTag(SerieDetailsScreenTestTags.SCREEN),
      topBar = {
        Column {
          CenterAlignedTopAppBar(
              title = {
                Text(
                    text = uiState.serie?.title ?: "Loading...",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag(SerieDetailsScreenTestTags.SERIE_TITLE))
              },
              navigationIcon = {
                IconButton(
                    onClick = onGoBack,
                    modifier = Modifier.testTag(SerieDetailsScreenTestTags.BACK_BUTTON)) {
                      Icon(
                          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                          contentDescription = "Back")
                    }
              },
              colors =
                  TopAppBarDefaults.topAppBarColors(
                      containerColor = MaterialTheme.colorScheme.surface))
          HorizontalDivider(color = outlineVariantLight, thickness = 1.dp)
        }
      }) { paddingValues ->
        if (uiState.isLoading) {
          // Loading state
          Box(
              modifier = Modifier.fillMaxSize().padding(paddingValues),
              contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag(SerieDetailsScreenTestTags.LOADING))
              }
        } else if (uiState.serie != null) {
          // Content state
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(paddingValues)
                      .padding(horizontal = 16.dp)
                      .padding(bottom = 16.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Spacer(modifier = Modifier.height(8.dp))

                // Meeting date and time
                Text(
                    text = "MEETING: ${uiState.formattedDateTime}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier.fillMaxWidth()
                            .testTag(SerieDetailsScreenTestTags.MEETING_INFO)
                            .padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)

                Spacer(modifier = Modifier.height(8.dp))

                // Info row: Visibility, Members, Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                      Text(
                          text = uiState.visibilityDisplay,
                          fontSize = 16.sp,
                          fontWeight = FontWeight.Medium,
                          color = MaterialTheme.colorScheme.onSurface,
                          modifier = Modifier.testTag(SerieDetailsScreenTestTags.VISIBILITY))

                      Text(
                          text = "MEMBERS : ${uiState.participantsCount}",
                          fontSize = 14.sp,
                          fontWeight = FontWeight.Medium,
                          color = MaterialTheme.colorScheme.onSurface,
                          modifier = Modifier.testTag(SerieDetailsScreenTestTags.MEMBERS_COUNT))

                      Text(
                          text = uiState.formattedDuration,
                          fontSize = 14.sp,
                          fontWeight = FontWeight.Medium,
                          color = MaterialTheme.colorScheme.onSurface,
                          modifier = Modifier.testTag(SerieDetailsScreenTestTags.DURATION))
                    }

                HorizontalDivider(thickness = 1.dp, color = outlineVariantLight)

                // Description
                Text(
                    text = uiState.serie?.description ?: "",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(min = 80.dp)
                            .testTag(SerieDetailsScreenTestTags.DESCRIPTION))

                HorizontalDivider(thickness = 1.dp, color = outlineVariantLight)

                // Events list in LazyColumn with fixed size
                if (uiState.events.isNotEmpty()) {
                  LazyColumn(
                      modifier =
                          Modifier.fillMaxWidth()
                              .weight(1f)
                              .testTag(SerieDetailsScreenTestTags.EVENT_LIST),
                      verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.events.size) { index ->
                          val event = uiState.events[index]
                          EventCard(
                              event = event,
                              onClick = { onEventCardClick(event.eventId) },
                              testTag = "${SerieDetailsScreenTestTags.EVENT_CARD}_${event.eventId}")
                        }
                      }
                } else {
                  // Empty state - same height as LazyColumn
                  Box(
                      modifier =
                          Modifier.fillMaxWidth()
                              .weight(1f) // Same height as LazyColumn
                              .testTag(SerieDetailsScreenTestTags.EVENT_LIST),
                      contentAlignment = Alignment.Center) {
                        Text(
                            text = "No events in this serie yet",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                      }
                }

                HorizontalDivider(thickness = 1.dp, color = outlineVariantLight)

                // Owner information
                Text(
                    text = "CREATED BY ${uiState.serie?.ownerId ?: "Unknown"}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier.fillMaxWidth()
                            .testTag(SerieDetailsScreenTestTags.OWNER_INFO)
                            .padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)

                // Add event button (only shown to owner)
                if (uiState.isOwner(currentUserId)) {
                  Button(
                      onClick = onAddEventClick,
                      modifier =
                          Modifier.fillMaxWidth()
                              .height(56.dp)
                              .testTag(SerieDetailsScreenTestTags.BUTTON_ADD_EVENT),
                      shape = RoundedCornerShape(8.dp),
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor = onBackgroundLight, contentColor = onPrimaryLight)) {
                        Text(text = "ADD EVENT", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                      }
                  Button(
                      onClick = onEditSerieClick,
                      modifier =
                          Modifier.fillMaxWidth()
                              .height(56.dp)
                              .testTag(SerieDetailsScreenTestTags.EDIT_SERIE_BUTTON),
                      shape = RoundedCornerShape(8.dp),
                      enabled = uiState.isOwner(currentUserId),
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor = onBackgroundLight, contentColor = onPrimaryLight)) {
                        Text(text = "EDIT SERIE", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                      }
                }

                // Join/Quit serie button (shown to non-owners)
                if (!uiState.isOwner(currentUserId)) {
                  Button(
                      onClick = {
                        coroutineScope.launch {
                          val success =
                              if (uiState.isParticipant(currentUserId)) {
                                serieDetailsViewModel.quitSerie((currentUserId))
                              } else {
                                serieDetailsViewModel.joinSerie(currentUserId)
                              }
                          if (success && !uiState.isParticipant(currentUserId)) {
                            // If user quit successfully, navigate back
                            onQuitSerieSuccess()
                          }
                        }
                      },
                      modifier =
                          Modifier.fillMaxWidth()
                              .height(56.dp)
                              .testTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE),
                      shape = RoundedCornerShape(8.dp),
                      enabled =
                          uiState.isParticipant(currentUserId) || uiState.canJoin(currentUserId),
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor = onBackgroundLight, contentColor = onPrimaryLight)) {
                        Text(
                            text =
                                if (uiState.isParticipant(currentUserId)) "QUIT SERIE"
                                else "JOIN SERIE",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium)
                      }
                }
              }
        }
      }
}
