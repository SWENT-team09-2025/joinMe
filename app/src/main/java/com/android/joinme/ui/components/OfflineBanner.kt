package com.android.joinme.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.joinme.network.NetworkMonitor

/**
 * Banner displayed at top of screen when offline. Shows a simple message: "You're offline. Viewing
 * cached data."
 *
 * Usage:
 * ```
 * Column {
 *   OfflineBanner()
 *   // Rest of your screen content
 * }
 * ```
 *
 * The banner only appears when the device has no network connectivity. When online, it takes up no
 * space.
 */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val networkMonitor = remember { NetworkMonitor(context) }
  val isOnline by
      networkMonitor.observeNetworkStatus().collectAsState(initial = networkMonitor.isOnline())

  if (!isOnline) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        tonalElevation = 2.dp) {
          Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "You're offline. Viewing cached data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer)
              }
        }
  }
}
