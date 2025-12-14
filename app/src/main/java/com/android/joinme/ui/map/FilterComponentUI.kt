package com.android.joinme.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.android.joinme.R
import com.android.joinme.model.filter.FilterState
import com.android.joinme.ui.theme.Dimens
import com.android.joinme.ui.theme.customColors

/**
 * Construct a clicked chip using in filter option
 *
 * @param name The name of the filter
 * @param filterState The current filter state
 * @param onToggleAction Callback for toggling the filter
 * @return The chip composable
 */
@Composable
private fun FilterChipConstructor(
    name: String,
    filterState: Boolean,
    onToggleAction: () -> Unit,
    testTag: String
) {
  FilterChip(
      selected = filterState,
      onClick = onToggleAction,
      label = { Text(name) },
      colors = MaterialTheme.customColors.filterChip,
      modifier = Modifier.testTag(testTag))
}

/**
 * Filter bottom sheet for the map screen.
 *
 * Displays event type filters (Social, Activity, Sport) and participation filters (My Events,
 * Joined Events, Other Events).
 *
 * @param filterState The current filter state
 * @param onToggleSocial Callback for toggling Social filter
 * @param onToggleActivity Callback for toggling Activity filter
 * @param onToggleSport Callback for toggling Sport filter
 * @param onToggleMyEvents Callback for toggling My Events filter
 * @param onToggleJoinedEvents Callback for toggling Joined Events filter
 * @param onToggleOtherEvents Callback for toggling Other Events filter
 * @param onClearFilters Callback for clearing all filters
 * @param onDismiss Callback when the bottom sheet is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    filterState: FilterState,
    onToggleSocial: () -> Unit,
    onToggleActivity: () -> Unit,
    onToggleSport: () -> Unit,
    onToggleMyEvents: () -> Unit,
    onToggleJoinedEvents: () -> Unit,
    onToggleOtherEvents: () -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState()

  ModalBottomSheet(
      onDismissRequest = onDismiss,
      sheetState = sheetState,
      modifier = Modifier.testTag(MapScreenTestTags.FILTER_BOTTOM_SHEET)) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = Dimens.Padding.medium)
                    .padding(bottom = Dimens.Padding.large)) {
              Text(
                  text = stringResource(R.string.type_object),
                  style = MaterialTheme.typography.titleMedium,
                  modifier = Modifier.padding(bottom = Dimens.Padding.small))

              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.small)) {
                    FilterChipConstructor(
                        name = stringResource(R.string.social_type),
                        filterState = filterState.isSocialSelected,
                        onToggleAction = onToggleSocial,
                        testTag = MapScreenTestTags.FILTER_TYPE_SOCIAL)
                    FilterChipConstructor(
                        name = stringResource(R.string.activity_type),
                        filterState = filterState.isActivitySelected,
                        onToggleAction = onToggleActivity,
                        testTag = MapScreenTestTags.FILTER_TYPE_ACTIVITY)
                    FilterChipConstructor(
                        name = stringResource(R.string.sport_type),
                        filterState = filterState.isSportSelected,
                        onToggleAction = onToggleSport,
                        testTag = MapScreenTestTags.FILTER_TYPE_SPORT)
                  }

              Spacer(modifier = Modifier.height(Dimens.Spacing.medium))
              HorizontalDivider()
              Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

              Text(
                  text = stringResource(R.string.participation),
                  style = MaterialTheme.typography.titleMedium,
                  modifier = Modifier.padding(bottom = Dimens.Padding.small))

              Column(
                  modifier = Modifier.fillMaxWidth(),
                  verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.small),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    FilterChipConstructor(
                        name = stringResource(R.string.my_events),
                        filterState = filterState.showMyEvents,
                        onToggleAction = onToggleMyEvents,
                        testTag = MapScreenTestTags.FILTER_PARTICIPATION_MY_EVENTS)

                    FilterChipConstructor(
                        name = stringResource(R.string.joined_events),
                        filterState = filterState.showJoinedEvents,
                        onToggleAction = onToggleJoinedEvents,
                        testTag = MapScreenTestTags.FILTER_PARTICIPATION_JOINED_EVENTS)
                    FilterChipConstructor(
                        name = stringResource(R.string.other_events),
                        filterState = filterState.showOtherEvents,
                        onToggleAction = onToggleOtherEvents,
                        testTag = MapScreenTestTags.FILTER_PARTICIPATION_OTHER_EVENTS)
                  }

              Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

              Button(
                  onClick = onClearFilters,
                  modifier =
                      Modifier.fillMaxWidth().testTag(MapScreenTestTags.FILTER_CLOSE_BUTTON)) {
                    Text(stringResource(R.string.clear_filters))
                  }
            }
      }
}
