package com.android.joinme.ui.overview

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.event.Event
import com.android.joinme.model.eventItem.EventItem
import com.android.joinme.model.filter.FilterState
import com.android.joinme.model.serie.Serie
import com.android.joinme.ui.components.EventCard
import com.android.joinme.ui.components.SerieCard
import com.android.joinme.ui.navigation.BottomNavigationMenu
import com.android.joinme.ui.navigation.NavigationActions
import com.android.joinme.ui.navigation.NavigationTestTags
import com.android.joinme.ui.navigation.Tab
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.customColors

/** Search text field with search and clear icons. */
@Composable
private fun SearchTextField(query: String, onQueryChange: (String) -> Unit) {
  val focusManager = LocalFocusManager.current
  OutlinedTextField(
      value = query,
      onValueChange = onQueryChange,
      placeholder = { Text("Search an event") },
      shape = RoundedCornerShape(Dimens.IconSize.large),
      leadingIcon = {
        IconButton(
            onClick = {
              if (query.isNotEmpty()) {
                // Perform search action here
                focusManager.clearFocus()
              }
            }) {
              Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
            }
      },
      trailingIcon = {
        if (query.isNotEmpty()) {
          IconButton(onClick = { onQueryChange("") }) {
            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
          }
        }
      },
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions =
          KeyboardActions(
              onSearch = {
                if (query.isNotEmpty()) {
                  // Perform search action here
                  focusManager.clearFocus()
                  // TODO later the search action
                }
              }),
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag(SearchScreenTestTags.SEARCH_TEXT_FIELD))
}

/** Filter chips row with Social, Activity, and Sport. */
@Composable
private fun FilterChipsRow(
    filterState: FilterState,
    onToggleSocial: () -> Unit,
    onToggleActivity: () -> Unit,
    onToggleSport: () -> Unit
) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.small)) {
        FilterChip(
            selected = filterState.isSocialSelected,
            onClick = onToggleSocial,
            label = { Text("Social") },
            colors = MaterialTheme.customColors.filterChip)

        FilterChip(
            selected = filterState.isActivitySelected,
            onClick = onToggleActivity,
            label = { Text("Activity") },
            colors = MaterialTheme.customColors.filterChip)

        FilterChip(
            selected = filterState.isSportSelected,
            onClick = onToggleSport,
            label = { Text("Sport") },
            colors = MaterialTheme.customColors.filterChip)
      }
}

/** Event list displaying search results. */
@Composable
private fun ColumnScope.SearchResultsList(
    eventItems: List<EventItem>,
    onSelectEvent: (Event) -> Unit,
    onSelectSerie: (String) -> Unit
) {
  LazyColumn(
      contentPadding = PaddingValues(vertical = Dimens.Padding.small),
      modifier =
          Modifier.fillMaxWidth()
              .weight(1f)
              .padding(horizontal = Dimens.Padding.medium)
              .testTag(SearchScreenTestTags.EVENT_LIST)) {
        items(eventItems.size) { index ->
          val item = eventItems[index]
          when (item) {
            is EventItem.SingleEvent -> {
              EventCard(
                  modifier = Modifier.padding(vertical = Dimens.Padding.small),
                  event = item.event,
                  onClick = { onSelectEvent(item.event) },
                  testTag = SearchScreenTestTags.getTestTagForEventItem(item.event))
            }
            is EventItem.EventSerie -> {
              SerieCard(
                  modifier = Modifier.padding(vertical = Dimens.Padding.small),
                  serie = item.serie,
                  onClick = { onSelectSerie(item.serie.serieId) },
                  testTag = SearchScreenTestTags.getTestTagForSerieItem(item.serie))
            }
          }
        }
      }
}

/** Empty state message when no results found. */
@Composable
private fun ColumnScope.EmptySearchMessage() {
  Column(
      modifier = Modifier.fillMaxSize().weight(1f),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "No events or series found. Try adjusting your filters or search query.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(SearchScreenTestTags.EMPTY_EVENT_LIST_MSG))
      }
}

object SearchScreenTestTags {
  const val SEARCH_TEXT_FIELD = "searchTextField"
  const val EVENT_LIST = "searchEventList"
  const val EMPTY_EVENT_LIST_MSG = "emptySearchEventList"

  fun getTestTagForEventItem(event: Event): String = "searchEventItem${event.eventId}"

  fun getTestTagForSerieItem(serie: Serie): String = "searchSerieItem${serie.serieId}"
}

/**
 * Search screen composable that displays a search interface with filters.
 *
 * Provides a search text field, filter chips (Social, Activity), and a sport category dropdown
 * menu. Users can search for events and series and apply various filters to narrow down results.
 *
 * @param searchViewModel ViewModel managing search state and filter logic
 * @param navigationActions Navigation actions for handling tab navigation
 * @param onSelectEvent Callback invoked when an event is selected from the list
 * @param onSelectSerie Callback invoked when a serie is selected from the list, receives serieId
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
    onSelectEvent: (Event) -> Unit = {},
    onSelectSerie: (String) -> Unit = {}
) {
  val context = LocalContext.current
  val uiState by searchViewModel.uiState.collectAsState()
  val filterState by searchViewModel.filterState.collectAsState()
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
        }
      },
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
                SearchTextField(
                    query = uiState.query, onQueryChange = { searchViewModel.setQuery(it) })

                FilterChipsRow(
                    filterState = filterState,
                    onToggleSocial = { searchViewModel.toggleSocial() },
                    onToggleActivity = { searchViewModel.toggleActivity() },
                    onToggleSport = { searchViewModel.toggleSport() })
              }

          if (eventItems.isNotEmpty()) {
            SearchResultsList(
                eventItems = eventItems,
                onSelectEvent = onSelectEvent,
                onSelectSerie = onSelectSerie)
          } else {
            EmptySearchMessage()
          }
        }
      }
}
