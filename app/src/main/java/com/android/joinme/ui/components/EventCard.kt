package com.android.joinme.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.getColor
import com.android.joinme.ui.theme.IconColor
import com.android.joinme.ui.theme.OnEventCardTextColor
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * A reusable card component for displaying event information.
 *
 * Displays event details including date, time, title, location, and an arrow indicator. The card is
 * styled with a color that corresponds to the event type (Sports, Social, Activity).
 *
 * The card is clickable and includes proper accessibility support for screen readers.
 *
 * @param modifier Modifier to be applied to the card. Use this to add custom padding, sizing, or
 *   other modifications from the caller.
 * @param event The event to display, containing title, date, location, and type information.
 * @param onClick Callback invoked when the card is clicked by the user.
 * @param testTag Test tag identifier for UI testing purposes.
 */
@Composable
fun EventCard(modifier: Modifier = Modifier, event: Event, onClick: () -> Unit, testTag: String) {
  Card(
      modifier = modifier.fillMaxWidth().clickable(onClick = onClick).testTag(testTag),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = event.type.getColor()),
      elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text =
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(event.date.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnEventCardTextColor)

                Text(
                    text =
                        SimpleDateFormat("HH'h'mm", Locale.getDefault())
                            .format(event.date.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnEventCardTextColor)
              }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
              text = event.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = OnEventCardTextColor)

          Spacer(modifier = Modifier.height(4.dp))

          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Place : ${event.location?.name ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnEventCardTextColor.copy(alpha = 0.9f))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "View event details",
                    tint = IconColor)
              }
        }
      }
}
