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
import com.android.joinme.model.eventItem.EventItem
import com.android.joinme.ui.components.EventCard
import com.android.joinme.ui.components.SerieCard
import com.android.joinme.ui.navigation.BottomNavigationMenu
import com.android.joinme.ui.navigation.NavigationActions
import com.android.joinme.ui.navigation.Tab

object SearchScreenTestTags {
  const val SEARCH_TEXT_FIELD = "searchTextField"
  const val EVENT_LIST = "searchEventList"
  const val EMPTY_EVENT_LIST_MSG = "emptySearchEventList"

  fun getTestTagForEventItem(event: Event): String = "searchEventItem${event.eventId}"
}

/**
 * Search screen composable that displays a search interface with filters.
 *
 * Provides a search text field, filter chips (Social, Activity), and a sport category dropdown
 * menu. Users can search for events and series, and apply various filters to narrow down results.
 * When no filters are selected, all events and series are shown.
 *
 * @param searchViewModel ViewModel managing search state and filter logic
 * @param searchQuery Initial search query (currently unused, reserved for future use)
 * @param navigationActions Navigation actions for handling tab navigation
 * @param onSelectEvent Callback invoked when an event is selected from the list
 * @param onSelectSerie Callback invoked when a serie is selected from the list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = viewModel(),
    searchQuery: String = "",
    navigationActions: NavigationActions? = null,
    onSelectEvent: (Event) -> Unit = {},
    onSelectSerie: (String) -> Unit = {}
) {
  val context = LocalContext.current
  val uiState by searchViewModel.uiState.collectAsState()
  val filterState by searchViewModel.filterState.collectAsState()
  val focusManager = LocalFocusManager.current
  val eventItems = uiState.eventItems

  LaunchedEffect(Unit) { searchViewModel.refreshUIState() }

  // Show error message if fetching todos fails
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      searchViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = { TopAppBar(title = { Text("Search") }) },
      bottomBar = {
        BottomNavigationMenu(
            selectedTab = Tab.Search,
            onTabSelected = { tab -> navigationActions?.navigateTo(tab.destination) },
            modifier = Modifier)
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
                    modifier =
                        Modifier.fillMaxWidth().testTag(SearchScreenTestTags.SEARCH_TEXT_FIELD))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      FilterChip(
                          selected = filterState.isSocialSelected,
                          onClick = { searchViewModel.toggleSocial() },
                          label = { Text("Social") })

                      FilterChip(
                          selected = filterState.isActivitySelected,
                          onClick = { searchViewModel.toggleActivity() },
                          label = { Text("Activity") })

                      // Dropdown filter
                      Box {
                        FilterChip(
                            selected = filterState.selectedSportsCount >= 1,
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
                                        checked = filterState.isSelectAllChecked,
                                        onCheckedChange = null)
                                  })

                              // Loop through all sport categories dynamically
                              filterState.sportCategories.forEach { sport ->
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

          if (eventItems.isNotEmpty()) {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier =
                    Modifier.fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .testTag(SearchScreenTestTags.EVENT_LIST)) {
                  items(eventItems.size) { index ->
                    when (val item = eventItems[index]) {
                      is EventItem.SingleEvent -> {
                        EventCard(
                            modifier = Modifier.padding(vertical = 6.dp),
                            event = item.event,
                            onClick = { onSelectEvent(item.event) },
                            testTag = SearchScreenTestTags.getTestTagForEventItem(item.event))
                      }
                      is EventItem.EventSerie -> {
                        SerieCard(
                            modifier = Modifier.padding(vertical = 6.dp),
                            serie = item.serie,
                            onClick = { onSelectSerie(item.serie.serieId) },
                            testTag = "searchSerieItem${item.serie.serieId}")
                      }
                    }
                  }
                }
          } else {
            Column(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                      text =
                          "No events or series found. Try adjusting your search or filters, or create your own event.",
                      textAlign = TextAlign.Center,
                      style = MaterialTheme.typography.bodyMedium,
                      modifier = Modifier.testTag(SearchScreenTestTags.EMPTY_EVENT_LIST_MSG))
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
