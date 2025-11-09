package com.android.joinme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.android.joinme.model.serie.Serie
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.customColors
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * A reusable card component for displaying serie information with a stacked cards effect.
 *
 * Displays serie details including date, time, title, and a "Serie 🔥" badge. The card is styled
 * with a dark background color and shows multiple layers behind it to indicate that it contains
 * multiple events.
 *
 * The card is clickable and includes proper accessibility support for screen readers.
 *
 * @param modifier Modifier to be applied to the card. Use this to add custom padding, sizing, or
 *   other modifications from the caller.
 * @param serie The serie to display, containing title, date, and other information.
 * @param onClick Callback invoked when the card is clicked by the user.
 * @param testTag Test tag identifier for UI testing purposes.
 */
@Composable
fun SerieCard(modifier: Modifier = Modifier, serie: Serie, onClick: () -> Unit, testTag: String) {
  Box(modifier = modifier.fillMaxWidth().padding(bottom = Dimens.SerieCard.bottomPadding)) {
    // Third layer (furthest back)
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = Dimens.SerieCard.thirdLayerHorizontalPadding)
                .offset(y = Dimens.SerieCard.thirdLayerOffset)
                .height(Dimens.SerieCard.cardHeight)
                .clip(RoundedCornerShape(Dimens.CornerRadius.large))
                .background(MaterialTheme.customColors.activity)
                .border(
                    Dimens.SerieCard.layerBorderWidth,
                    MaterialTheme.customColors.activity,
                    RoundedCornerShape(Dimens.CornerRadius.large)))

    // Second layer (middle)
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = Dimens.SerieCard.secondLayerHorizontalPadding)
                .offset(y = Dimens.SerieCard.secondLayerOffset)
                .height(Dimens.SerieCard.cardHeight)
                .clip(RoundedCornerShape(Dimens.CornerRadius.large))
                .background(MaterialTheme.customColors.sports)
                .border(
                    Dimens.SerieCard.layerBorderWidth,
                    MaterialTheme.customColors.sports,
                    RoundedCornerShape(Dimens.CornerRadius.large)))

    // Main card (front)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag(testTag),
        shape = RoundedCornerShape(Dimens.CornerRadius.large),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.customColors.serieContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.Elevation.large)) {
          Column(modifier = Modifier.fillMaxWidth().padding(Dimens.Padding.medium)) {
            // Top row: date, Serie badge, time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                  Text(
                      text =
                          SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                              .format(serie.date.toDate()),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.customColors.onSerieContainer)

                  Text(
                      text = "Serie 🔥",
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Medium,
                      color = MaterialTheme.customColors.onSerieContainer)

                  Text(
                      text =
                          SimpleDateFormat("H'h'mm", Locale.getDefault())
                              .format(serie.date.toDate()),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.customColors.onSerieContainer)
                }

            Spacer(modifier = Modifier.height(Dimens.Spacing.small))

            // Title
            Text(
                text = serie.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.customColors.onSerieContainer)

            Spacer(modifier = Modifier.height(Dimens.Spacing.extraSmall))

            // Bottom row: arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                      contentDescription = "View serie details",
                      tint = MaterialTheme.colorScheme.onSurface)
                }
          }
        }
  }
}
