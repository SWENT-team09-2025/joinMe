package com.android.joinme.ui.overview

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

object CreateSerieScreenTestTags {
  const val INPUT_SERIE_TITLE = "inputSerieTitle"
  const val INPUT_SERIE_DESCRIPTION = "inputSerieDescription"
  const val INPUT_SERIE_MAX_PARTICIPANTS = "inputSerieMaxParticipants"
  const val INPUT_SERIE_DATE = "inputSerieDate"
  const val INPUT_SERIE_TIME = "inputSerieTime"
  const val INPUT_SERIE_VISIBILITY = "inputSerieVisibility"
  const val BUTTON_SAVE_SERIE = "buttonSaveSerie"
  const val ERROR_MESSAGE = "errorMessage"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSerieScreen(
    createSerieViewModel: CreateSerieViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onDone: () -> Unit = {}
) {
  val uiState by createSerieViewModel.uiState.collectAsState()
  val errorMsg = uiState.errorMsg
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      createSerieViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Create Serie") },
            navigationIcon = {
              IconButton(onClick = onGoBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            })
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
              Spacer(modifier = Modifier.height(8.dp))

              // Title field
              OutlinedTextField(
                  value = uiState.title,
                  onValueChange = { createSerieViewModel.setTitle(it) },
                  label = { Text("Title") },
                  modifier =
                      Modifier.fillMaxWidth().testTag(CreateSerieScreenTestTags.INPUT_SERIE_TITLE),
                  isError = uiState.invalidTitleMsg != null,
                  supportingText = {
                    if (uiState.invalidTitleMsg != null) {
                      Text(
                          text = uiState.invalidTitleMsg!!, color = MaterialTheme.colorScheme.error)
                    }
                  },
                  singleLine = true)

              // Description field
              OutlinedTextField(
                  value = uiState.description,
                  onValueChange = { createSerieViewModel.setDescription(it) },
                  label = { Text("Description") },
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(120.dp)
                          .testTag(CreateSerieScreenTestTags.INPUT_SERIE_DESCRIPTION),
                  isError = uiState.invalidDescriptionMsg != null,
                  supportingText = {
                    if (uiState.invalidDescriptionMsg != null) {
                      Text(
                          text = uiState.invalidDescriptionMsg!!,
                          color = MaterialTheme.colorScheme.error)
                    }
                  },
                  maxLines = 4)

              // Max Participants field
              OutlinedTextField(
                  value = uiState.maxParticipants,
                  onValueChange = { createSerieViewModel.setMaxParticipants(it) },
                  label = { Text("Max Participant") },
                  modifier =
                      Modifier.width(180.dp)
                          .testTag(CreateSerieScreenTestTags.INPUT_SERIE_MAX_PARTICIPANTS),
                  isError = uiState.invalidMaxParticipantsMsg != null,
                  supportingText = {
                    if (uiState.invalidMaxParticipantsMsg != null) {
                      Text(
                          text = uiState.invalidMaxParticipantsMsg!!,
                          color = MaterialTheme.colorScheme.error)
                    }
                  },
                  singleLine = true)

              // Date and Time row
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Date field
                    OutlinedTextField(
                        value = uiState.date,
                        onValueChange = { createSerieViewModel.setDate(it) },
                        label = { Text("Date") },
                        placeholder = { Text("dd/MM/yyyy") },
                        modifier =
                            Modifier.weight(1f).testTag(CreateSerieScreenTestTags.INPUT_SERIE_DATE),
                        isError = uiState.invalidDateMsg != null,
                        supportingText = {
                          if (uiState.invalidDateMsg != null) {
                            Text(
                                text = uiState.invalidDateMsg!!,
                                color = MaterialTheme.colorScheme.error)
                          }
                        },
                        singleLine = true)

                    // Time field
                    OutlinedTextField(
                        value = uiState.time,
                        onValueChange = { createSerieViewModel.setTime(it) },
                        label = { Text("Time") },
                        placeholder = { Text("HH:mm") },
                        modifier =
                            Modifier.weight(1f).testTag(CreateSerieScreenTestTags.INPUT_SERIE_TIME),
                        isError = uiState.invalidTimeMsg != null,
                        supportingText = {
                          if (uiState.invalidTimeMsg != null) {
                            Text(
                                text = uiState.invalidTimeMsg!!,
                                color = MaterialTheme.colorScheme.error)
                          }
                        },
                        singleLine = true)
                  }

              // Serie Visibility field
              OutlinedTextField(
                  value = uiState.visibility,
                  onValueChange = { createSerieViewModel.setVisibility(it) },
                  label = { Text("Serie Visibility") },
                  placeholder = { Text("PUBLIC or PRIVATE") },
                  modifier =
                      Modifier.fillMaxWidth()
                          .testTag(CreateSerieScreenTestTags.INPUT_SERIE_VISIBILITY),
                  isError = uiState.invalidVisibilityMsg != null,
                  supportingText = {
                    if (uiState.invalidVisibilityMsg != null) {
                      Text(
                          text = uiState.invalidVisibilityMsg!!,
                          color = MaterialTheme.colorScheme.error)
                    }
                  },
                  singleLine = true)

              Spacer(modifier = Modifier.height(16.dp))

              // Next button
              Button(
                  onClick = {
                    coroutineScope.launch {
                      if (createSerieViewModel.createSerie()) {
                        onDone()
                      }
                    }
                  },
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(56.dp)
                          .testTag(CreateSerieScreenTestTags.BUTTON_SAVE_SERIE),
                  enabled = uiState.isValid && !uiState.isLoading) {
                    if (uiState.isLoading) {
                      CircularProgressIndicator(
                          modifier = Modifier.size(24.dp),
                          color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                      Text("Next")
                    }
                  }

              Spacer(modifier = Modifier.height(16.dp))
            }
      }
}
