package com.android.joinme.ui.overview

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.ui.components.EventCard
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.buttonColors
import com.android.joinme.ui.theme.customColors
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

object SerieDetailsScreenTestTags {
  const val SCREEN = "serieDetailsScreen"
  const val SERIE_TITLE = "serieTitle"
  const val MEETING_INFO = "meetingInfo"
  const val VISIBILITY = "visibility"
  const val MEMBERS_COUNT = "membersCount"
  const val DURATION = "duration"
  const val DESCRIPTION = "description"
  const val EVENT_LIST = "eventList"
  const val EVENT_CARD = "eventCard"
  const val OWNER_INFO = "ownerInfo"
  const val BUTTON_ADD_EVENT = "buttonAddEvent"
  const val BUTTON_QUIT_SERIE = "buttonQuitSerie"
  const val LOADING = "loading"
  const val BACK_BUTTON = "backButton"
  const val EDIT_SERIE_BUTTON = "editSerieButton"
  const val DELETE_SERIE_BUTTON = "deleteSerieButton"
  const val MESSAGE_FULL_SERIE = "messageFullSerie"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SerieDetailsScreen(
    serieId: String,
    serieDetailsViewModel: SerieDetailsViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onEventCardClick: (String) -> Unit = {},
    onAddEventClick: () -> Unit = {},
    onQuitSerieSuccess: () -> Unit = {},
    onEditSerieClick: (String) -> Unit = {},
    currentUserId: String = Firebase.auth.currentUser?.uid ?: "unknown"
) {
  val uiState by serieDetailsViewModel.uiState.collectAsState()
  val errorMsg = uiState.errorMsg
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var showDeleteDialog by remember { mutableStateOf(false) }

  // Load serie details when the screen is first displayed
  LaunchedEffect(serieId) { serieDetailsViewModel.loadSerieDetails(serieId) }

  // Show error messages as toasts
  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      serieDetailsViewModel.clearErrorMsg()
    }
  }

  var ownerDisplayName by remember { mutableStateOf("...") }
  // Only use LaunchedEffect to fetch owner display name (no composable calls here)
  LaunchedEffect(uiState.serie?.ownerId) {
    uiState.serie?.ownerId?.let { id ->
      ownerDisplayName = serieDetailsViewModel.getOwnerDisplayName(id)
    }
  }

  // Delete confirmation dialog (composable placed at top level)
  if (showDeleteDialog) {
    AlertDialog(
        onDismissRequest = { showDeleteDialog = false },
        title = { Text("Delete Serie") },
        text = {
          Text("Are you sure you want to delete this serie? This action cannot be undone.")
        },
        confirmButton = {
          TextButton(
              onClick = {
                coroutineScope.launch {
                  serieDetailsViewModel.deleteSerie(serieId)
                  showDeleteDialog = false
                  onGoBack()
                }
              }) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
              }
        },
        dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } })
  }

  // Main UI scaffold (now outside LaunchedEffect)
  Scaffold(
      modifier = Modifier.testTag(SerieDetailsScreenTestTags.SCREEN),
      topBar = {
        Column {
          CenterAlignedTopAppBar(
              title = {
                Text(
                    text = uiState.serie?.title ?: "Loading...",
                    style = MaterialTheme.typography.titleLarge,
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
          HorizontalDivider(
              thickness = Dimens.BorderWidth.thin, color = MaterialTheme.colorScheme.primary)
        }
      }) { paddingValues ->
        if (uiState.isLoading) {
          Box(
              modifier = Modifier.fillMaxSize().padding(paddingValues),
              contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag(SerieDetailsScreenTestTags.LOADING))
              }
        } else if (uiState.serie != null) {
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(paddingValues)
                      .padding(horizontal = Dimens.Spacing.large),
              verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.medium),
          ) {
            Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

            Text(
                text = "MEETING: ${uiState.formattedDateTime}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().testTag(SerieDetailsScreenTestTags.MEETING_INFO),
                textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                  Text(
                      text = uiState.visibilityDisplay,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.testTag(SerieDetailsScreenTestTags.VISIBILITY))

                  Text(
                      text = "MEMBERS : ${uiState.participantsCount}",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.testTag(SerieDetailsScreenTestTags.MEMBERS_COUNT))

                  Text(
                      text = uiState.formattedDuration,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.testTag(SerieDetailsScreenTestTags.DURATION))
                }

            HorizontalDivider(
                thickness = Dimens.BorderWidth.thin, color = MaterialTheme.colorScheme.primary)

            Text(
                text = uiState.serie?.description ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.fillMaxWidth()
                        .heightIn(min = Dimens.Spacing.large)
                        .testTag(SerieDetailsScreenTestTags.DESCRIPTION))

            HorizontalDivider(
                thickness = Dimens.BorderWidth.thin, color = MaterialTheme.colorScheme.primary)

            if (uiState.events.isNotEmpty()) {
              LazyColumn(
                  modifier =
                      Modifier.fillMaxWidth()
                          .weight(1f)
                          .testTag(SerieDetailsScreenTestTags.EVENT_LIST),
                  verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.medium),
              ) {
                items(uiState.events.size) { index ->
                  val event = uiState.events[index]
                  EventCard(
                      event = event,
                      onClick = { onEventCardClick(event.eventId) },
                      testTag = "${SerieDetailsScreenTestTags.EVENT_CARD}_${event.eventId}")
                }
              }
            } else {
              Box(
                  modifier =
                      Modifier.fillMaxWidth()
                          .weight(1f)
                          .testTag(SerieDetailsScreenTestTags.EVENT_LIST),
                  contentAlignment = Alignment.Center) {
                    Text(
                        text = "No events in this serie yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center)
                  }
            }

            HorizontalDivider(
                thickness = Dimens.BorderWidth.thin, color = MaterialTheme.colorScheme.primary)

            Text(
                text = "Created by $ownerDisplayName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier.fillMaxWidth()
                        .testTag(SerieDetailsScreenTestTags.OWNER_INFO)
                        .padding(vertical = Dimens.Spacing.small),
                textAlign = TextAlign.Center)

            if (uiState.isOwner(currentUserId)) {
              Button(
                  onClick = onAddEventClick,
                  enabled = uiState.events.size < 30,
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(Dimens.Spacing.huge)
                          .testTag(SerieDetailsScreenTestTags.BUTTON_ADD_EVENT),
                  shape = RoundedCornerShape(Dimens.CornerRadius.medium),
                  colors = MaterialTheme.customColors.buttonColors()) {
                    Text(text = "ADD EVENT", style = MaterialTheme.typography.headlineSmall)
                  }

              Button(
                  onClick = { onEditSerieClick(serieId) },
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(Dimens.Spacing.huge)
                          .testTag(SerieDetailsScreenTestTags.EDIT_SERIE_BUTTON),
                  shape = RoundedCornerShape(Dimens.CornerRadius.medium),
                  enabled = uiState.isOwner(currentUserId),
                  colors = MaterialTheme.customColors.buttonColors()) {
                    Text(text = "EDIT SERIE", style = MaterialTheme.typography.headlineSmall)
                  }

              Button(
                  onClick = { showDeleteDialog = true },
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(Dimens.Spacing.huge)
                          .testTag(SerieDetailsScreenTestTags.DELETE_SERIE_BUTTON),
                  shape = RoundedCornerShape(Dimens.CornerRadius.medium),
                  colors = MaterialTheme.customColors.buttonColors()) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.customColors.deleteButton)
                    Spacer(modifier = Modifier.width(Dimens.Spacing.small))
                    Text(text = "DELETE SERIE", style = MaterialTheme.typography.headlineSmall)
                  }
            }

            if (!uiState.isOwner(currentUserId)) {
              if (uiState.canJoin(currentUserId) || uiState.isParticipant(currentUserId)) {
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
                          onQuitSerieSuccess()
                        }
                      }
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(Dimens.Spacing.huge)
                            .testTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE),
                    shape = RoundedCornerShape(Dimens.CornerRadius.medium),
                    enabled =
                        uiState.isParticipant(currentUserId) || uiState.canJoin(currentUserId),
                    colors = MaterialTheme.customColors.buttonColors()) {
                      Text(
                          text =
                              if (uiState.isParticipant(currentUserId)) "QUIT SERIE"
                              else "JOIN SERIE",
                          style = MaterialTheme.typography.headlineSmall)
                    }
              } else {
                Text(
                    text = "Sorry this serie is full",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(bottom = Dimens.Padding.extraLarge)
                            .testTag(SerieDetailsScreenTestTags.MESSAGE_FULL_SERIE),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
}
