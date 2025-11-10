package com.android.joinme.ui.overview

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.buttonColors
import com.android.joinme.ui.theme.customColors
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

/**
 * Test tags for the ShowEventScreen UI components.
 *
 * These constants are used to identify UI elements in instrumented tests, enabling reliable test
 * assertions and interactions.
 */
object ShowEventScreenTestTags {
  const val SCREEN = "showEventScreen"
  const val EVENT_TYPE = "eventType"
  const val EVENT_TITLE = "eventTitle"
  const val EVENT_DATE = "eventDate"
  const val EVENT_VISIBILITY = "eventVisibility"
  const val EVENT_DESCRIPTION = "eventDescription"
  const val EVENT_LOCATION = "eventLocation"
  const val EVENT_MEMBERS = "eventMembers"
  const val EVENT_DURATION = "eventDuration"
  const val EVENT_OWNER = "eventOwner"
  const val JOIN_QUIT_BUTTON = "joinQuitButton"
  const val EDIT_BUTTON = "editButton"
  const val DELETE_BUTTON = "deleteButton"
  const val FULL_EVENT_MESSAGE = "fullEventMessage"
}

/**
 * Screen that displays detailed information about a specific event.
 *
 * This screen shows comprehensive event details including type, title, description, location, date,
 * duration, visibility, participants, and owner information. The UI adapts based on the current
 * user's relationship to the event:
 * - **Event owners** see Edit and Delete buttons to manage their event
 * - **Other users** see a Join/Quit button to manage their participation
 * - **Past events** show no action buttons
 *
 * The screen includes a confirmation dialog for event deletion and displays error messages via
 * toasts when operations fail.
 *
 * @param eventId The unique identifier of the event to display
 * @param currentUserId The ID of the currently authenticated user (defaults to Firebase auth user)
 * @param showEventViewModel ViewModel managing event state and operations
 * @param onGoBack Callback invoked when the user navigates back
 * @param onEditEvent Callback invoked when the owner wants to edit the event, receives the event ID
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowEventScreen(
    eventId: String,
    currentUserId: String = Firebase.auth.currentUser?.uid ?: "unknown",
    showEventViewModel: ShowEventViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onEditEvent: (String) -> Unit = {}
) {
  LaunchedEffect(eventId) { showEventViewModel.loadEvent(eventId) }

  val eventUIState by showEventViewModel.uiState.collectAsState()
  val errorMsg = eventUIState.errorMsg

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var showDeleteDialog by remember { mutableStateOf(false) }

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      showEventViewModel.clearErrorMsg()
    }
  }

  // Delete confirmation dialog
  if (showDeleteDialog) {
    AlertDialog(
        onDismissRequest = { showDeleteDialog = false },
        title = { Text("Delete Event") },
        text = {
          Text("Are you sure you want to delete this event? This action cannot be undone.")
        },
        confirmButton = {
          TextButton(
              onClick = {
                coroutineScope.launch {
                  showEventViewModel.deleteEvent(eventId)
                  showDeleteDialog = false
                  onGoBack()
                }
              }) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
              }
        },
        dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } })
  }

  Scaffold(
      modifier = Modifier.testTag(ShowEventScreenTestTags.SCREEN),
      topBar = {
        Column {
          CenterAlignedTopAppBar(
              title = {
                Text(
                    text = eventUIState.title.ifBlank { "Task title" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag(ShowEventScreenTestTags.EVENT_TITLE))
              },
              navigationIcon = {
                IconButton(onClick = onGoBack) {
                  Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                      contentDescription = "Back",
                      tint = MaterialTheme.colorScheme.primary)
                }
              },
              colors =
                  TopAppBarDefaults.topAppBarColors(
                      containerColor = MaterialTheme.colorScheme.surface))
          HorizontalDivider(
              color = MaterialTheme.colorScheme.primary, thickness = Dimens.BorderWidth.thin)
        }
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.Padding.medium)
                    .padding(bottom = Dimens.Padding.medium),
            verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.medium)) {
              Spacer(modifier = Modifier.height(Dimens.Spacing.small))

              // Date display
              Text(
                  text = eventUIState.date,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier =
                      Modifier.fillMaxWidth()
                          .testTag(ShowEventScreenTestTags.EVENT_DATE)
                          .padding(vertical = Dimens.Padding.small),
                  textAlign = TextAlign.Center)

              Spacer(modifier = Modifier.height(Dimens.Spacing.small))

              // Visibility and Type row
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = eventUIState.visibility,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag(ShowEventScreenTestTags.EVENT_VISIBILITY))

                    Text(
                        text = eventUIState.type,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag(ShowEventScreenTestTags.EVENT_TYPE))
                  }

              HorizontalDivider(
                  thickness = Dimens.BorderWidth.thin, color = MaterialTheme.colorScheme.primary)

              // Description
              Text(
                  text = eventUIState.description,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier =
                      Modifier.fillMaxWidth()
                          .heightIn(min = Dimens.ShowEvent.minDescriptionField)
                          .testTag(ShowEventScreenTestTags.EVENT_DESCRIPTION))

              HorizontalDivider(
                  thickness = Dimens.BorderWidth.thin, color = MaterialTheme.colorScheme.primary)

              // Location
              Text(
                  text = eventUIState.location,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier =
                      Modifier.fillMaxWidth().testTag(ShowEventScreenTestTags.EVENT_LOCATION))

              HorizontalDivider(
                  thickness = Dimens.BorderWidth.thin, color = MaterialTheme.colorScheme.primary)

              // Members and Duration row
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text =
                            "MEMBERS : ${eventUIState.participantsCount}/${eventUIState.maxParticipants}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag(ShowEventScreenTestTags.EVENT_MEMBERS))

                    Text(
                        text = "${eventUIState.duration}min",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag(ShowEventScreenTestTags.EVENT_DURATION))
                  }

              HorizontalDivider(
                  thickness = Dimens.BorderWidth.thin, color = MaterialTheme.colorScheme.primary)

              // Owner display
              Text(
                  text = eventUIState.ownerName,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier =
                      Modifier.fillMaxWidth()
                          .testTag(ShowEventScreenTestTags.EVENT_OWNER)
                          .padding(vertical = Dimens.Padding.small),
                  textAlign = TextAlign.Center)

              Spacer(modifier = Modifier.weight(1f))

              // Conditional buttons based on ownership and event status
              // Only show buttons if the event is not in the past
              if (!eventUIState.isPastEvent) {
                if (eventUIState.isOwner(currentUserId)) {
                  // Owner sees: Edit and Delete buttons
                  Button(
                      onClick = { onEditEvent(eventId) },
                      modifier =
                          Modifier.fillMaxWidth()
                              .height(Dimens.Button.standardHeight)
                              .testTag(ShowEventScreenTestTags.EDIT_BUTTON),
                      shape = RoundedCornerShape(Dimens.CornerRadius.medium),
                      colors = MaterialTheme.customColors.buttonColors()) {
                        Text(
                            text = "EDIT EVENT",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium)
                      }

                  OutlinedButton(
                      onClick = { showDeleteDialog = true },
                      modifier =
                          Modifier.fillMaxWidth()
                              .height(Dimens.Button.standardHeight)
                              .testTag(ShowEventScreenTestTags.DELETE_BUTTON),
                      shape = RoundedCornerShape(Dimens.CornerRadius.medium),
                      colors = MaterialTheme.customColors.buttonColors()) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.customColors.deleteButton)
                        Spacer(modifier = Modifier.width(Dimens.Spacing.small))
                        Text(
                            text = "DELETE EVENT",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium)
                      }
                } else {
                  // if event is full, don't display join button
                  val participantCount = eventUIState.participantsCount.toIntOrNull() ?: 0
                  val maxParticipants = eventUIState.maxParticipants.toIntOrNull() ?: Int.MAX_VALUE
                  if ((participantCount < maxParticipants) ||
                      eventUIState.isParticipant(currentUserId)) {
                    // Non-owner sees: Join/Quit button
                    Button(
                        onClick = {
                          coroutineScope.launch {
                            showEventViewModel.toggleParticipation(eventId, currentUserId)
                          }
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(Dimens.Button.standardHeight)
                                .testTag(ShowEventScreenTestTags.JOIN_QUIT_BUTTON),
                        shape = RoundedCornerShape(Dimens.CornerRadius.medium),
                        colors = MaterialTheme.customColors.buttonColors()) {
                          Text(
                              text =
                                  if (eventUIState.isParticipant(currentUserId)) "QUIT EVENT"
                                  else "JOIN EVENT",
                              style = MaterialTheme.typography.headlineSmall,
                              fontWeight = FontWeight.Medium)
                        }
                  } else {
                    Text(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(bottom = 50.dp)
                                .testTag(ShowEventScreenTestTags.FULL_EVENT_MESSAGE),
                        text = "Sorry the event:\n ${eventUIState.title} \n is full",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
      }
}
