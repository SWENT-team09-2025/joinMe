package com.android.joinme.ui.groups

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.R
import com.android.joinme.model.event.EventType
import com.android.joinme.model.event.displayString
import com.android.joinme.model.event.getColor
import com.android.joinme.model.profile.Profile

/**
 * Main screen for displaying group details including members and events.
 *
 * @param groupId The unique identifier of the group to display.
 * @param viewModel The ViewModel managing the screen state and data.
 * @param onBackClick Callback when the back button is clicked.
 * @param onGroupEventsClick Callback when the "Group Events" button is clicked.
 * @param onMemberClick Callback when a member profile is clicked, receives the member's UID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    viewModel: GroupDetailViewModel = viewModel(factory = GroupDetailViewModelFactory()),
    onBackClick: () -> Unit = {},
    onGroupEventsClick: () -> Unit = {},
    onMemberClick: (String) -> Unit = {}
) {
  LaunchedEffect(groupId) { viewModel.loadGroupDetails(groupId) }

  val uiState by viewModel.uiState.collectAsState()

  Scaffold(
      topBar = {
        TopAppBar(
            title = {},
            navigationIcon = {
              IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            },
            actions = {
              // Only show category when group is loaded
              if (uiState.group != null) {
                Text(
                    text = uiState.group!!.category.displayString(),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 16.dp))
              }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    // Use group category color if loaded, otherwise default
                    containerColor =
                        uiState.group?.category?.getColor() ?: MaterialTheme.colorScheme.surface))
      }) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          when {
            uiState.isLoading -> {
              CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.error != null -> {
              ErrorContent(
                  message = uiState.error ?: "Unknown error",
                  onRetry = { viewModel.loadGroupDetails(groupId) },
                  modifier = Modifier.align(Alignment.Center))
            }
            uiState.group != null -> {
              GroupContent(
                  groupCategory = uiState.group!!.category,
                  groupName = uiState.group!!.name,
                  groupDescription = uiState.group!!.description,
                  members = uiState.members,
                  membersCount = uiState.group!!.membersCount,
                  onGroupEventsClick = onGroupEventsClick,
                  onMemberClick = onMemberClick)
            }
          }
        }
      }
}

/** Main content displaying group information and members. */
@Composable
private fun GroupContent(
    groupCategory: EventType,
    groupName: String,
    groupDescription: String,
    members: List<Profile>,
    membersCount: Int,
    onGroupEventsClick: () -> Unit,
    onMemberClick: (String) -> Unit
) {
  Column(modifier = Modifier.fillMaxSize().background(groupCategory.getColor())) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
      Box(
          modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.White),
          contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.group_default_picture),
                contentDescription = "JoinMe App Logo",
                modifier = Modifier.size(175.dp),
                contentScale = ContentScale.Fit)
          }

      Spacer(modifier = Modifier.height(24.dp))

      Text(text = groupName, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)

      Spacer(modifier = Modifier.height(8.dp))

      Text(text = groupDescription, fontSize = 16.sp, color = Color.White)

      Spacer(modifier = Modifier.height(24.dp))
    }

    Box(
        modifier =
            Modifier.weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(Color.Transparent)
                .border(width = 2.dp, color = Color.Black, shape = RoundedCornerShape(12.dp))
                .padding(16.dp)) {
          LazyColumn(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(members) { member ->
                  MemberItem(profile = member, onClick = { onMemberClick(member.uid) })
                }
              }
        }

    Spacer(modifier = Modifier.height(16.dp))

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
      Button(
          onClick = onGroupEventsClick,
          modifier = Modifier.fillMaxWidth().height(56.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
          shape = RoundedCornerShape(28.dp)) {
            Text(text = "Group Events", fontSize = 18.sp, fontWeight = FontWeight.Medium)
          }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
          text = "members : $membersCount",
          fontSize = 14.sp,
          color = Color.Black,
          modifier = Modifier.align(Alignment.End))

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun MemberItem(profile: Profile, onClick: () -> Unit = {}) {
  Row(
      modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Start) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Profile photo of ${profile.username}",
            modifier = Modifier.size(48.dp),
            tint = Color.White.copy(alpha = 0.7f))

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = profile.username,
            fontSize = 18.sp,
            color = Color.White,
            fontWeight = FontWeight.Normal)
      }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
      }
}
