package com.android.joinme.ui.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.joinme.model.group.Group
import com.android.joinme.viewmodel.GroupListUIState

object GroupListScreenTestTags {
  const val TITLE = "groups:title"
  const val ADD_NEW_GROUP = "groups:addNewGroup"
  const val LIST = "groups:list"
  const val EMPTY = "groups:empty"

  fun cardTag(id: String) = "group:card:$id"

  fun moreTag(id: String) = "group:more:$id"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    uiState: GroupListUIState,
    onJoinANewGroup: () -> Unit = {},
    onGroup: (Group) -> Unit = {},
    onMoreOptionMenu: (Group) -> Unit = {}
) {
  val groups = uiState.groups
  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              Text(
                  "Your groups",
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.testTag(GroupListScreenTestTags.TITLE))
            })
      },
      floatingActionButton = {
        ExtendedFloatingActionButton(
            modifier = Modifier.testTag(GroupListScreenTestTags.ADD_NEW_GROUP),
            onClick = onJoinANewGroup,
            icon = {
              Icon(Icons.Default.Add, contentDescription = "Join a new group", tint = Color.White)
            },
            text = { Text("Join a new group", color = Color.White) },
            containerColor = Color.Black)
      },
      floatingActionButtonPosition = FabPosition.Center,
  ) { pd ->
    if (groups.isNotEmpty()) {
      LazyColumn(
          modifier =
              Modifier.fillMaxSize()
                  .padding(horizontal = 16.dp)
                  .padding(bottom = pd.calculateBottomPadding() + 84.dp)
                  .padding(top = pd.calculateTopPadding())
                  .testTag(GroupListScreenTestTags.LIST),
          contentPadding = PaddingValues(vertical = 12.dp)) {
            items(groups) { group ->
              GroupCard(
                  group = group,
                  onClick = { onGroup(group) },
                  onMoreOptions = { onMoreOptionMenu(group) })
              Spacer(Modifier.height(12.dp))
            }
          }
    } else {
      Box(
          modifier = Modifier.fillMaxSize().padding(pd).testTag(GroupListScreenTestTags.EMPTY),
          contentAlignment = Alignment.Center) {
            Text(
                text = "You are currently not\nassigned to a group…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
          }
    }
  }
}

@Composable
private fun GroupCard(group: Group, onClick: () -> Unit, onMoreOptions: () -> Unit) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .heightIn(min = 86.dp)
              .clickable { onClick() }
              .testTag(GroupListScreenTestTags.cardTag(group.id)),
      colors = CardDefaults.cardColors(containerColor = Color(0xFFE06B60))) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White)
            Spacer(Modifier.height(4.dp))
            if (group.description.isNotBlank()) {
              Text(
                  text = group.description,
                  style = MaterialTheme.typography.bodySmall,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                  color = Color.White.copy(alpha = 0.9f))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "members: ${group.membersCount}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f))
          }
          IconButton(
              onClick = onMoreOptions,
              modifier = Modifier.testTag(GroupListScreenTestTags.moreTag(group.id))) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = Color.White)
              }
        }
      }
}

@Preview(showBackground = true)
@Composable
private fun Test() {
  val sample =
      listOf(
          Group(id = "1", name = "Test", category = "X", description = "12345", membersCount = 25),
          Group(
              id = "2",
              name = "Gregory le singe",
              category = "Y",
              description = "6789A",
              membersCount = 17))
  GroupListScreen(uiState = GroupListUIState(groups = sample))
}

@Preview(showBackground = true)
@Composable
private fun Test2() {
  GroupListScreen(uiState = GroupListUIState(groups = emptyList()))
}
