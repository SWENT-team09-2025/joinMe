package com.android.joinme.ui.overview

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.event.Event

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = viewModel(),
    searchQuery: String = "",
    onGoBack: () -> Unit,
    onSelectEvent: (Event) -> Unit = {}
) {
  val context = LocalContext.current
  val uiState by searchViewModel.uiState.collectAsState()
  val focusManager = LocalFocusManager.current
  val events = uiState.events

  // Debug: Check events list
  LaunchedEffect(events) {
    android.util.Log.d("SearchScreen", "Events count: ${events.size}")
    events.forEach { event ->
      android.util.Log.d("SearchScreen", "Event: ${event.title} - ${event.type}")
    }
  }

  // Don't call refreshUIState() when events are already set from MainActivity
   LaunchedEffect(Unit) { searchViewModel.refreshUIState() }

  // Show error message if fetching todos fails
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      searchViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Search") },
            navigationIcon = {
              IconButton(onClick = onGoBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            })
      }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { searchViewModel.setQuery(it) },
                    placeholder = { Text("Search an event") },
                    shape = RoundedCornerShape(30.dp),
                    leadingIcon = {
                      IconButton(
                          onClick = {
                            if (uiState.query.isNotEmpty()) {
                              // Perform search action here
                              focusManager.clearFocus()
                            }
                          }) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                          }
                    },
                    trailingIcon = {
                      if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { searchViewModel.setQuery("") }) {
                          Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                      }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions =
                        KeyboardActions(
                            onSearch = {
                              if (uiState.query.isNotEmpty()) {
                                // Perform search action here
                                focusManager.clearFocus()
                                // TODO later the search action
                              }
                            }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("searchTextField"))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      FilterChip(
                          selected = uiState.isAllSelected,
                          onClick = { searchViewModel.toggleAll() },
                          label = { Text("All") })

                      FilterChip(
                          selected = uiState.isSocialSelected,
                          onClick = { searchViewModel.toggleSocial() },
                          label = { Text("Social") })

                      FilterChip(
                          selected = uiState.isActivitySelected,
                          onClick = { searchViewModel.toggleActivity() },
                          label = { Text("Activity") })

                      // Dropdown filter
                      Box {
                        FilterChip(
                            selected = uiState.selectedSportsCount >= 1,
                            onClick = { searchViewModel.setCategoryExpanded(true) },
                            label = { Text("Sport") },
                            trailingIcon = {
                              Icon(
                                  imageVector = Icons.Default.ArrowDropDown,
                                  contentDescription = "Dropdown")
                            })

                        DropdownMenu(
                            expanded = uiState.categoryExpanded,
                            onDismissRequest = { searchViewModel.setCategoryExpanded(false) }) {
                              DropdownMenuItem(
                                  text = { Text("Select all") },
                                  onClick = { searchViewModel.toggleSelectAll() },
                                  trailingIcon = {
                                    Checkbox(
                                        checked = uiState.isSelectAllChecked,
                                        onCheckedChange = null)
                                  })

                              // Loop through all sport categories dynamically
                              uiState.sportCategories.forEach { sport ->
                                DropdownMenuItem(
                                    text = { Text(sport.name) },
                                    onClick = { searchViewModel.toggleSport(sport.id) },
                                    trailingIcon = {
                                      Checkbox(checked = sport.isChecked, onCheckedChange = null)
                                    })
                              }
                            }
                      }
                    }
              }

          if (events.isNotEmpty()) {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier =
                    Modifier.fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .testTag(OverviewScreenTestTags.EVENT_LIST)) {
                  items(events.size) { index ->
                    val event = events[index]
                    EventCard(event = event, onClick = { onSelectEvent(event) })
                  }
                }
          } else {
            Column(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                      text = "You have no events yet. Join one, or create your own event.",
                      textAlign = TextAlign.Center,
                      style = MaterialTheme.typography.bodyMedium,
                      modifier = Modifier.testTag(OverviewScreenTestTags.EMPTY_EVENT_LIST_MSG))
                }
          }
        }
      }
}

// @Preview(showBackground = true)
// @Composable
// fun SearchScreenPreview() {
//  SearchScreen(onGoBack = {})
// }
