package com.android.joinme.ui.groups

import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.group.Group
import com.android.joinme.viewmodel.GroupListViewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    groupListViewModel: GroupListViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onJoinANewGroup: () -> Unit = {},
    onGroup: () -> Unit = {},
    onMoreOptionMenu: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by groupListViewModel.uiState.collectAsState()
    val groups = uiState.groups

    LaunchedEffect(Unit) { groupListViewModel.refreshUIState() }

    LaunchedEffect(uiState.errorMsg) {
        uiState.errorMsg?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            groupListViewModel.clearErrorMsg()
        }
    }

    Scaffold(
        topBar = {CenterAlignedTopAppBar(title = {Text("Your groups")})},

        floatingActionButton = { ExtendedFloatingActionButton(
            onClick = onJoinANewGroup,
            icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Join a new group") },
            text = { Text("Join a new group") }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { pd ->
        if (groups.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = pd.calculateBottomPadding() + 84.dp)
                    .padding(top = pd.calculateTopPadding()),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(groups) { group ->
                    GroupCard(
                        group = group,
                        onClick = onGroup,
                        onMoreOptions = onMoreOptionMenu
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pd),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "You are currently not\nassigned to a group…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}


@Composable
private fun GroupCard(
    group: Group,
    onClick: () -> Unit,
    onMoreOptions: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 86.dp)
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                if (group.description.isNotBlank()) {
                    Text(
                        text = group.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "members : ${group.membersCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            IconButton(onClick = onMoreOptions) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White)
            }
        }
    }
}