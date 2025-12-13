package com.android.joinme.ui.overview

import android.annotation.SuppressLint
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.R
import com.android.joinme.model.invitation.InvitationType
import com.android.joinme.ui.components.EventCard
import com.android.joinme.ui.components.ShareButton
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.buttonColors
import com.android.joinme.ui.theme.customColors
import com.android.joinme.util.TestEnvironmentDetector
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Note: this file was co-written with the help of AI (Claude) */
private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000.0

/**
 * Gets the current user ID with test environment support.
 *
 * @return The current user's UID, or "test-user-id" in test environments
 */
private fun getCurrentUserIdForSerieDetails(): String {
  return Firebase.auth.currentUser?.uid
      ?: if (TestEnvironmentDetector.shouldUseTestUserId()) TestEnvironmentDetector.getTestUserId()
      else "unknown"
}

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

  /** Test tag for the group info text */
  const val GROUP_INFO = "groupInfo"

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

  /** Test tag for the delete serie button */
  const val DELETE_SERIE_BUTTON = "deleteSerieButton"

  /** Test tag for the full serie message */
  const val MESSAGE_FULL_SERIE = "messageFullSerie"
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
 * @param onEditSerieClick Callback invoked when the "Edit serie" button is clicked, receives the
 *   serie ID
 * @param currentUserId The ID of the currently logged-in user
 */
@SuppressLint("UnrememberedMutableState")
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
    currentUserId: String = getCurrentUserIdForSerieDetails()
) {
  val uiState by serieDetailsViewModel.uiState.collectAsState()
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var showDeleteDialog by remember { mutableStateOf(false) }
  var ownerDisplayName by remember { mutableStateOf("...") }

  // Load serie details and show error messages
  SerieDetailsEffects(
      serieId = serieId,
      errorMsg = uiState.errorMsg,
      ownerId = uiState.serie?.ownerId,
      viewModel = serieDetailsViewModel,
      context = context,
      onOwnerNameLoaded = { ownerDisplayName = it })

  // Delete confirmation dialog
  ShowDeleteWindow(
      showDeleteDialog = showDeleteDialog,
      onDismiss = { showDeleteDialog = false },
      onConfirm = { showDeleteDialog = false },
      onGoBack = onGoBack,
      serieId = serieId,
      serieDetailsViewModel = serieDetailsViewModel,
      coroutineScope = coroutineScope)

  // Main UI scaffold
  Scaffold(
      modifier = Modifier.testTag(SerieDetailsScreenTestTags.SCREEN),
      topBar = {
        SerieDetailsTopBar(
            serieTitle = uiState.serie?.title ?: stringResource(R.string.loading),
            serieId = serieId,
            currentUserId = currentUserId,
            serieDate = uiState.serie?.date,
            isPastSerie = uiState.isPastSerie,
            groupId = uiState.groupId,
            onGoBack = onGoBack)
      }) { paddingValues ->
        when {
          uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(
                      modifier = Modifier.testTag(SerieDetailsScreenTestTags.LOADING))
                }
          }
          uiState.serie != null -> {
            SerieDetailsContent(
                uiState = uiState,
                paddingValues = paddingValues,
                ownerDisplayName = ownerDisplayName,
                currentUserId = currentUserId,
                serieId = serieId,
                serieDetailsViewModel = serieDetailsViewModel,
                onEventCardClick = onEventCardClick,
                onAddEventClick = onAddEventClick,
                onEditSerieClick = onEditSerieClick,
                onQuitSerieSuccess = onQuitSerieSuccess,
                onShowDeleteDialog = { showDeleteDialog = true },
                coroutineScope = coroutineScope)
          }
        }
      }
}

/** Handles LaunchedEffects for loading data and showing errors. */
@Composable
private fun SerieDetailsEffects(
    serieId: String,
    errorMsg: String?,
    ownerId: String?,
    viewModel: SerieDetailsViewModel,
    context: android.content.Context,
    onOwnerNameLoaded: (String) -> Unit
) {
  LaunchedEffect(serieId) { viewModel.loadSerieDetails(serieId) }

  LaunchedEffect(errorMsg) {
    errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      viewModel.clearErrorMsg()
    }
  }

  LaunchedEffect(ownerId) {
    ownerId?.let { id -> onOwnerNameLoaded(viewModel.getOwnerDisplayName(id)) }
  }
}

/** Top bar for serie details screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SerieDetailsTopBar(
    serieTitle: String,
    serieId: String,
    currentUserId: String,
    serieDate: Timestamp?,
    isPastSerie: Boolean,
    groupId: String?,
    onGoBack: () -> Unit
) {
  Column {
    CenterAlignedTopAppBar(
        title = {
          Text(
              text = serieTitle,
              style = MaterialTheme.typography.titleLarge,
              modifier = Modifier.testTag(SerieDetailsScreenTestTags.SERIE_TITLE))
        },
        navigationIcon = {
          IconButton(
              onClick = onGoBack,
              modifier = Modifier.testTag(SerieDetailsScreenTestTags.BACK_BUTTON)) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
        },
        actions = {
          if (serieDate != null && !isPastSerie && groupId == null) {
            val daysUntilSerie =
                ((serieDate.toDate().time - System.currentTimeMillis()) / MILLIS_PER_DAY)
                    .coerceAtLeast(0.0)

            ShareButton(
                invitationType = InvitationType.SERIE,
                targetId = serieId,
                createdBy = currentUserId,
                expiresInDays = daysUntilSerie)
          }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
    HorizontalDivider(
        thickness = Dimens.BorderWidth.thin, color = MaterialTheme.colorScheme.primary)
  }
}

/** Serie information display (meeting time, visibility, members, duration, description). */
@Composable
private fun SerieInfo(uiState: SerieDetailsUIState) {
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

  HorizontalDivider(thickness = Dimens.BorderWidth.thin, color = MaterialTheme.colorScheme.primary)

  Text(
      text = uiState.serie?.description ?: "",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier =
          Modifier.fillMaxWidth()
              .heightIn(min = Dimens.Spacing.large)
              .testTag(SerieDetailsScreenTestTags.DESCRIPTION))

  HorizontalDivider(thickness = Dimens.BorderWidth.thin, color = MaterialTheme.colorScheme.primary)
}

/** Events list or empty state. */
@Composable
private fun ColumnScope.EventsList(
    events: List<com.android.joinme.model.event.Event>,
    onEventCardClick: (String) -> Unit
) {
  if (events.isNotEmpty()) {
    LazyColumn(
        modifier =
            Modifier.fillMaxWidth().weight(1f).testTag(SerieDetailsScreenTestTags.EVENT_LIST),
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.medium)) {
          items(events.size) { index ->
            val event = events[index]
            EventCard(
                event = event,
                onClick = { onEventCardClick(event.eventId) },
                testTag = "${SerieDetailsScreenTestTags.EVENT_CARD}_${event.eventId}")
          }
        }
  } else {
    Box(
        modifier =
            Modifier.fillMaxWidth().weight(1f).testTag(SerieDetailsScreenTestTags.EVENT_LIST),
        contentAlignment = Alignment.Center) {
          Text(
              text = "No events in this serie yet",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center)
        }
  }
}

/** Action buttons based on user role. */
@Composable
private fun SerieActionButtons(
    uiState: SerieDetailsUIState,
    currentUserId: String,
    serieId: String,
    serieDetailsViewModel: SerieDetailsViewModel,
    onAddEventClick: () -> Unit,
    onEditSerieClick: (String) -> Unit,
    onQuitSerieSuccess: () -> Unit,
    onShowDeleteDialog: () -> Unit,
    coroutineScope: CoroutineScope
) {
  if (!uiState.isPastSerie) {
    when {
      uiState.isOwner(currentUserId) -> {
        OwnerActionButtons(
            serieId = serieId,
            serieDetailsViewModel = serieDetailsViewModel,
            onAddEventClick = onAddEventClick,
            onEditSerieClick = onEditSerieClick,
            onShowDeleteDialog = onShowDeleteDialog,
            currentUserId = currentUserId)
      }
      else -> {
        ParticipantActionButtons(
            currentUserId = currentUserId,
            serieDetailsViewModel = serieDetailsViewModel,
            onQuitSerieSuccess = onQuitSerieSuccess,
            coroutineScope = coroutineScope)
      }
    }
  }
}

/** Content shown when serie is loaded. */
@Composable
private fun SerieDetailsContent(
    uiState: SerieDetailsUIState,
    paddingValues: PaddingValues,
    ownerDisplayName: String,
    currentUserId: String,
    serieId: String,
    serieDetailsViewModel: SerieDetailsViewModel,
    onEventCardClick: (String) -> Unit,
    onAddEventClick: () -> Unit,
    onEditSerieClick: (String) -> Unit,
    onQuitSerieSuccess: () -> Unit,
    onShowDeleteDialog: () -> Unit,
    coroutineScope: CoroutineScope
) {
  Column(
      modifier =
          Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = Dimens.Spacing.large),
      verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.medium)) {
        Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

        SerieInfo(uiState = uiState)

        EventsList(events = uiState.events, onEventCardClick = onEventCardClick)

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

        uiState.groupName?.let { groupName ->
          Text(
              text = stringResource(R.string.group_name, groupName),
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface,
              modifier =
                  Modifier.fillMaxWidth()
                      .testTag(SerieDetailsScreenTestTags.GROUP_INFO)
                      .padding(vertical = Dimens.Spacing.small),
              textAlign = TextAlign.Center)
        }

        SerieActionButtons(
            uiState = uiState,
            currentUserId = currentUserId,
            serieId = serieId,
            serieDetailsViewModel = serieDetailsViewModel,
            onAddEventClick = onAddEventClick,
            onEditSerieClick = onEditSerieClick,
            onQuitSerieSuccess = onQuitSerieSuccess,
            onShowDeleteDialog = onShowDeleteDialog,
            coroutineScope = coroutineScope)

        Spacer(modifier = Modifier.height(Dimens.Spacing.small))
      }
}

/** Owner action buttons (Add Event, Edit Serie, Delete Serie). */
@Composable
private fun OwnerActionButtons(
    serieId: String,
    serieDetailsViewModel: SerieDetailsViewModel,
    onAddEventClick: () -> Unit,
    onEditSerieClick: (String) -> Unit,
    onShowDeleteDialog: () -> Unit,
    currentUserId: String
) {
  val uiState by serieDetailsViewModel.uiState.collectAsState()

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
      onClick = onShowDeleteDialog,
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

/** Handles the join/quit serie action. */
private suspend fun handleJoinQuitAction(
    currentUserId: String,
    serieDetailsViewModel: SerieDetailsViewModel,
    onQuitSerieSuccess: () -> Unit
) {
  val uiState = serieDetailsViewModel.uiState.value
  val success =
      if (uiState.isParticipant(currentUserId)) {
        serieDetailsViewModel.quitSerie((currentUserId))
      } else {
        serieDetailsViewModel.joinSerie(currentUserId)
      }
  if (success && !serieDetailsViewModel.uiState.value.isParticipant(currentUserId)) {
    // If user quit successfully, navigate back
    onQuitSerieSuccess()
  }
}

/** Participant action button (Join/Quit). */
@Composable
private fun ParticipantActionButtons(
    currentUserId: String,
    serieDetailsViewModel: SerieDetailsViewModel,
    onQuitSerieSuccess: () -> Unit,
    coroutineScope: CoroutineScope
) {
  val uiState by serieDetailsViewModel.uiState.collectAsState()

  if (uiState.canJoin(currentUserId) || uiState.isParticipant(currentUserId)) {
    Button(
        onClick = {
          coroutineScope.launch {
            handleJoinQuitAction(currentUserId, serieDetailsViewModel, onQuitSerieSuccess)
          }
        },
        modifier =
            Modifier.fillMaxWidth()
                .height(Dimens.Spacing.huge)
                .testTag(SerieDetailsScreenTestTags.BUTTON_QUIT_SERIE),
        shape = RoundedCornerShape(Dimens.CornerRadius.medium),
        enabled = uiState.isParticipant(currentUserId) || uiState.canJoin(currentUserId),
        colors = MaterialTheme.customColors.buttonColors()) {
          Text(
              text = if (uiState.isParticipant(currentUserId)) "QUIT SERIE" else "JOIN SERIE",
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

@Composable
fun ShowDeleteWindow(
    showDeleteDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onGoBack: () -> Unit,
    serieId: String,
    serieDetailsViewModel: SerieDetailsViewModel,
    coroutineScope: CoroutineScope
) {
  if (showDeleteDialog) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Serie") },
        text = {
          Text("Are you sure you want to delete this serie? This action cannot be undone.")
        },
        confirmButton = {
          TextButton(
              onClick = {
                coroutineScope.launch {
                  serieDetailsViewModel.deleteSerie(serieId)
                  onConfirm()
                  onGoBack()
                }
              }) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
              }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
  }
}
