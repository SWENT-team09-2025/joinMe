package com.android.joinme.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.android.joinme.model.event.Event


object OverviewScreenTestTags {
    const val CREATE_EVENT_BUTTON = "createEventFab"
    const val EMPTY_EVENT_LIST_MSG = "emptyEventList"
    const val EVENT_LIST = "eventList"

    fun getTestTagForEventItem(event: Event): String = "todoItem${event.eventId}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    // overviewViewModel: OverviewViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onSignedOut: () -> Unit = {},
    onSelectTodo: (Event) -> Unit = {},
    onAddTodo: () -> Unit = {},
    // navController: NavHostController = rememberNavController()l,
) {

  val context = LocalContext.current

  Scaffold(
      topBar = {
        Column {
          TopAppBar(
              title = {
                Text(
                    text = "Welcome, Mathieu",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center)
              },
              colors =
                  TopAppBarDefaults.topAppBarColors(
                      containerColor = MaterialTheme.colorScheme.surface))
          Divider(color = Color.Black, thickness = 1.dp)
        }
      },
      bottomBar = {
        NavigationBar {
          NavigationBarItem(
              selected = true,
              onClick = { /* TODO: menu */},
              icon = { Icon(Icons.Default.Menu, contentDescription = "Menu") })
          NavigationBarItem(
              selected = false,
              onClick = { /* TODO: search */},
              icon = { Icon(Icons.Default.Search, contentDescription = "Search") })
          NavigationBarItem(
              selected = false,
              onClick = { /* TODO: my events */},
              icon = { Icon(Icons.Filled.Place, contentDescription = "Events") })
          NavigationBarItem(
              selected = false,
              onClick = { /* TODO: profile */},
              icon = { Icon(Icons.Default.Person, contentDescription = "Profile") })
        }
      },
      floatingActionButton = {
        FloatingActionButton(
            onClick = onAddTodo,
            containerColor = Color(0xFFEDE7F6) // light purple like your screenshot
            ) {
              Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
      }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
            // contentAlignment = Alignment.Center
            ) {
              Text(
                  text = "You have no events yet. Join one, or",
                  textAlign = TextAlign.Center,
                  style = MaterialTheme.typography.bodyMedium)
              Text(
                  text = "create your own event",
                  textAlign = TextAlign.Center,
                  style = MaterialTheme.typography.bodyMedium)
            }
      }
}

@Preview
@Composable
fun OverviewScreenPreview() {
  OverviewScreen()
}
