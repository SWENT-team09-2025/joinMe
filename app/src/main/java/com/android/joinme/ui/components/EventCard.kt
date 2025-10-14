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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.getColor
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun EventCard(event: Event, onClick: () -> Unit, testTag: String) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 6.dp, horizontal = 8.dp)
              .testTag(testTag)
              .clickable(onClick = onClick),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = event.type.getColor()),
      elevation = CardDefaults.cardElevation(6.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text =
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(event.date.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White)

                Text(
                    text =
                        SimpleDateFormat("HH'h'mm", Locale.getDefault())
                            .format(event.date.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White)
              }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
              text = event.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White)

          Spacer(modifier = Modifier.height(4.dp))

          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Place : ${event.location?.name ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White)
              }
        }
      }
}
