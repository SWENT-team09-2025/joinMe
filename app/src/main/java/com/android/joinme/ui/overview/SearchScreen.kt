package com.android.joinme.ui.overview

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.event.Event
import com.android.joinme.ui.components.EventCard
import com.android.joinme.ui.navigation.BottomNavigationMenu
import com.android.joinme.ui.navigation.NavigationActions
import com.android.joinme.ui.navigation.NavigationTestTags
import com.android.joinme.ui.navigation.Tab
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.customColors

object SearchScreenTestTags {
  const val SEARCH_TEXT_FIELD = "searchTextField"
  const val EVENT_LIST = "searchEventList"
  const val EMPTY_EVENT_LIST_MSG = "emptySearchEventList"

  fun getTestTagForEventItem(event: Event): String = "searchEventItem${event.eventId}"
}

/**
 * Search screen composable that displays a search interface with filters.
 *
 * Provides a search text field, filter chips (All, Social, Activity), and a sport category dropdown
 * menu. Users can search for events and apply various filters to narrow down results.
 *
 * @param searchViewModel ViewModel managing search state and filter logic
 * @param searchQuery Initial search query (currently unused, reserved for future use)
 * @param navigationActions Navigation actions for handling tab navigation
 * @param onSelectEvent Callback invoked when an event is selected from the list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = viewModel(),
    searchQuery: String = "",
    navigationActions: NavigationActions? = null,
    onSelectEvent: (Event) -> Unit = {}
) {
  val context = LocalContext.current
  val uiState by searchViewModel.uiState.collectAsState()
  val filterState by searchViewModel.filterState.collectAsState()
  val focusManager = LocalFocusManager.current
  val events = uiState.events

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
          Column {
              CenterAlignedTopAppBar(
                  modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE),
                  title = { Text(text = "Search", style = MaterialTheme.typography.titleLarge) },
                  colors =
                      TopAppBarDefaults.topAppBarColors(
                          containerColor = MaterialTheme.colorScheme.surface))
              HorizontalDivider(
                  color = MaterialTheme.colorScheme.primary, thickness = Dimens.BorderWidth.thin)
          } },
      bottomBar = {
        BottomNavigationMenu(
            selectedTab = Tab.Search,
            onTabSelected = { tab -> navigationActions?.navigateTo(tab.destination) },
            modifier = Modifier)
      }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          Column(
              modifier = Modifier.padding(Dimens.Padding.medium),
              verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.small)) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { searchViewModel.setQuery(it) },
                    placeholder = { Text("Search an event") },
                    shape = RoundedCornerShape(Dimens.IconSize.large),
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
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.small)) {
                      FilterChip(
                          selected = filterState.isAllSelected,
                          onClick = { searchViewModel.toggleAll() },
                          label = { Text("All") },
                          colors = MaterialTheme.customColors.filterChip)

                      FilterChip(
                          selected = filterState.isSocialSelected,
                          onClick = { searchViewModel.toggleSocial() },
                          label = { Text("Social") },
                          colors = MaterialTheme.customColors.filterChip)

                      FilterChip(
                          selected = filterState.isActivitySelected,
                          onClick = { searchViewModel.toggleActivity() },
                          label = { Text("Activity") },
                          colors = MaterialTheme.customColors.filterChip)

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
                            },
                            colors = MaterialTheme.customColors.filterChip)

                        DropdownMenu(
                            expanded = uiState.categoryExpanded,
                            onDismissRequest = { searchViewModel.setCategoryExpanded(false) },
                            modifier =
                                Modifier.background(MaterialTheme.customColors.backgroundMenu)) {
                              DropdownMenuItem(
                                  text = { Text("Select all") },
                                  onClick = { searchViewModel.toggleSelectAll() },
                                  trailingIcon = {
                                    Checkbox(
                                        checked = filterState.isSelectAllChecked,
                                        onCheckedChange = null)
                                  },
                                  colors = MaterialTheme.customColors.dropdownMenu)

                              // Loop through all sport categories dynamically
                              filterState.sportCategories.forEach { sport ->
                                DropdownMenuItem(
                                    text = { Text(sport.name) },
                                    onClick = { searchViewModel.toggleSport(sport.id) },
                                    trailingIcon = {
                                      Checkbox(checked = sport.isChecked, onCheckedChange = null)
                                    },
                                    colors = MaterialTheme.customColors.dropdownMenu)
                              }
                            }
                      }
                    }
              }

          if (events.isNotEmpty()) {
            LazyColumn(
                contentPadding = PaddingValues(vertical = Dimens.Padding.small),
                modifier =
                    Modifier.fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = Dimens.Padding.medium)
                        .testTag(SearchScreenTestTags.EVENT_LIST)) {
                  items(events.size) { index ->
                    val event = events[index]
                    EventCard(
                        modifier = Modifier.padding(vertical = Dimens.Padding.small),
                        event = event,
                        onClick = { onSelectEvent(event) },
                        testTag = SearchScreenTestTags.getTestTagForEventItem(event))
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
                      modifier = Modifier.testTag(SearchScreenTestTags.EMPTY_EVENT_LIST_MSG))
                }
          }
        }
      }
}
