package com.android.joinme.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = viewModel(),
    searchQuery: String = "",
    onGoBack: () -> Unit
) {
  val uiState by searchViewModel.uiState.collectAsState()
  val focusManager = LocalFocusManager.current

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Search") },
            navigationIcon = {
              IconButton(onClick = onGoBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            })
      }) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
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
                            }
                          }),
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth())

              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.isAllSelected,
                        onClick = { searchViewModel.toggleAll() },
                        label = { Text("All") })

                    FilterChip(
                        selected = uiState.isBarSelected,
                        onClick = { searchViewModel.toggleBar() },
                        label = { Text("Bar") })

                    FilterChip(
                        selected = uiState.isClubSelected,
                        onClick = { searchViewModel.toggleClub() },
                        label = { Text("Club") })

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
                                      checked = uiState.isSelectAllChecked, onCheckedChange = null)
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
      }
}

// @Preview(showBackground = true)
// @Composable
// fun SearchScreenPreview() {
//  SearchScreen(onGoBack = {})
// }
