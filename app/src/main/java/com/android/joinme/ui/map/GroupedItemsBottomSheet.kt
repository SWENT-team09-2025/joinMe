package com.android.joinme.ui.map

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.android.joinme.R
import com.android.joinme.ui.theme.Dimens

private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
private const val COLUMN_WEIGHT = 1f

/**
 * Displays a single item row in the grouped items list.
 *
 * @param item The map item to display
 * @param onItemClick Callback when the item is clicked
 * @param context The Android context for starting activities
 */
@Composable
internal fun GroupedItemRow(item: MapItem, onItemClick: (MapItem) -> Unit, context: Context) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Padding.smallMedium),
      verticalAlignment = Alignment.CenterVertically) {
        // Color indicator
        Surface(
            modifier = Modifier.size(Dimens.Padding.medium),
            shape = MaterialTheme.shapes.small,
            color = item.color) {}

        Spacer(modifier = Modifier.width(Dimens.Padding.smallMedium))

        // Title and category - clickable to see details
        Column(modifier = Modifier.weight(COLUMN_WEIGHT).clickable { onItemClick(item) }) {
          Text(
              text = item.title,
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurface)
          Text(
              text =
                  when (item) {
                    is MapItem.EventItem ->
                        item.event.type.name.lowercase().replaceFirstChar { it.uppercase() }
                    is MapItem.SerieItem -> stringResource(R.string.serie)
                  },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Directions button (Navigation)
        FloatingActionButton(
            onClick = {
              val uri = "google.navigation:q=${item.position.latitude},${item.position.longitude}"
              val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri.toUri())
              intent.setPackage(GOOGLE_MAPS_PACKAGE)
              try {
                context.startActivity(intent)
              } catch (e: android.content.ActivityNotFoundException) {
                val fallbackUri =
                    "geo:${item.position.latitude},${item.position.longitude}?q=${item.position.latitude},${item.position.longitude}"
                val fallbackIntent =
                    android.content.Intent(android.content.Intent.ACTION_VIEW, fallbackUri.toUri())
                context.startActivity(fallbackIntent)
              }
            },
            modifier = Modifier.size(Dimens.Button.googleMapButtonBackGround),
            containerColor = MaterialTheme.colorScheme.secondaryContainer) {
              Icon(
                  imageVector = Icons.Filled.Directions,
                  contentDescription = stringResource(R.string.directions),
                  modifier = Modifier.size(Dimens.Button.googleMapButtonIcon),
                  tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }

        Spacer(modifier = Modifier.width(Dimens.Spacing.small))

        // View in Maps button
        FloatingActionButton(
            onClick = {
              val uri =
                  "geo:${item.position.latitude},${item.position.longitude}?q=${item.position.latitude},${item.position.longitude}(${item.title})"
              val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri.toUri())
              intent.setPackage(GOOGLE_MAPS_PACKAGE)
              try {
                context.startActivity(intent)
              } catch (e: android.content.ActivityNotFoundException) {
                intent.setPackage(null)
                context.startActivity(intent)
              }
            },
            modifier = Modifier.size(Dimens.Button.googleMapButtonBackGround),
            containerColor = MaterialTheme.colorScheme.primaryContainer) {
              Icon(
                  imageVector = Icons.Filled.Map,
                  contentDescription = stringResource(R.string.view_on_map),
                  modifier = Modifier.size(Dimens.Button.googleMapButtonIcon),
                  tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
      }
}

/**
 * Displays a bottom sheet with a list of events/series at the same location.
 *
 * @param group The MapMarkerGroup containing items to display
 * @param onItemClick Callback when an item is clicked
 * @param onDismiss Callback when the bottom sheet is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupedItemsBottomSheet(
    group: MapMarkerGroup,
    onItemClick: (MapItem) -> Unit,
    onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState()

  ModalBottomSheet(
      onDismissRequest = onDismiss,
      sheetState = sheetState,
      modifier = Modifier.testTag(MapScreenTestTags.GROUPED_INFO_WINDOW)) {
        Column(modifier = Modifier.fillMaxWidth().padding(Dimens.Padding.medium)) {
          Text(
              text =
                  stringResource(
                      R.string.number_of_activities_with_same_location, group.items.size),
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(Dimens.Padding.medium))

          HorizontalDivider(modifier = Modifier.padding(Dimens.Padding.small))

          // List of all items
          group.items.forEachIndexed { index, item ->
            Box(modifier = Modifier.testTag(MapScreenTestTags.getTestTagForGroupedItem(index))) {
              GroupedItemRow(item = item, onItemClick = onItemClick, context = context)
            }

            if (index < group.items.size - 1) {
              HorizontalDivider()
            }
          }

          Spacer(modifier = Modifier.height(Dimens.Spacing.sectionSpacing))
        }
      }
}
