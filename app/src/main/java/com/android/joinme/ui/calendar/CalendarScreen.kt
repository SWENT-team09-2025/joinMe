package com.android.joinme.ui.calendar

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.joinme.model.event.Event
import com.android.joinme.model.eventItem.EventItem
import com.android.joinme.model.serie.Serie
import com.android.joinme.ui.components.EventCard
import com.android.joinme.ui.components.SerieCard
import com.android.joinme.ui.theme.Dimens
import java.util.*

/** Note: This file was co-written with AI (Claude). */

/** Test tags for Calendar screen UI elements. */
object CalendarScreenTestTags {
    const val SCREEN = "calendarScreen"
    const val TOP_BAR = "calendarTopBar"
    const val BACK_BUTTON = "calendarBackButton"
    const val MONTH_YEAR_TEXT = "calendarMonthYearText"
    const val PREVIOUS_MONTH_BUTTON = "calendarPreviousMonthButton"
    const val NEXT_MONTH_BUTTON = "calendarNextMonthButton"
    const val CALENDAR_GRID = "calendarGrid"
    const val UPCOMING_EVENTS_SECTION = "upcomingEventsSection"
    const val EMPTY_STATE_TEXT = "calendarEmptyStateText"

    fun dayCell(day: Int) = "calendarDay_$day"
    fun eventCard(itemId: String) = "calendarEventCard_$itemId"
}

/**
 * Main Calendar screen displaying a calendar view and upcoming events.
 *
 * @param calendarViewModel The CalendarViewModel managing the state
 * @param onGoBack Callback invoked when the user taps the back button
 * @param onSelectEvent Callback invoked when a user taps on an event card
 * @param onSelectSerie Callback invoked when a user taps on a serie card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    calendarViewModel: CalendarViewModel = viewModel(),
    onGoBack: () -> Unit = {},
    onSelectEvent: (Event) -> Unit = {},
    onSelectSerie: (Serie) -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by calendarViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            calendarViewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag(CalendarScreenTestTags.SCREEN),
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    IconButton(
                        onClick = onGoBack,
                        modifier = Modifier.testTag(CalendarScreenTestTags.BACK_BUTTON)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                modifier = Modifier.testTag(CalendarScreenTestTags.TOP_BAR))
        }) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Dimens.Padding.medium)) {
            // Month/Year header with navigation
            MonthYearHeader(
                currentMonth = uiState.currentMonth,
                currentYear = uiState.currentYear,
                onPreviousMonth = { calendarViewModel.previousMonth() },
                onNextMonth = { calendarViewModel.nextMonth() })

            Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

            // Calendar grid
            CalendarGrid(
                currentMonth = uiState.currentMonth,
                currentYear = uiState.currentYear,
                selectedDate = uiState.selectedDate,
                daysWithItems = uiState.daysWithItems,
                onDateSelected = { timestamp -> calendarViewModel.selectDate(timestamp) })

            Spacer(modifier = Modifier.height(Dimens.Spacing.large))

            // Upcoming events section
            Text(
                text = "Upcoming events",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag(CalendarScreenTestTags.UPCOMING_EVENTS_SECTION))

            Spacer(modifier = Modifier.height(Dimens.Spacing.medium))

            // Events list or empty state
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.itemsForDate.isEmpty()) {
                EmptyStateMessage()
            } else {
                EventsList(
                    items = uiState.itemsForDate,
                    onSelectEvent = onSelectEvent,
                    onSelectSerie = onSelectSerie)
            }
        }
    }
}

/**
 * Month/Year header with navigation buttons.
 *
 * @param currentMonth The current month (0-11)
 * @param currentYear The current year
 * @param onPreviousMonth Callback when previous month button is clicked
 * @param onNextMonth Callback when next month button is clicked
 */
@Composable
private fun MonthYearHeader(
    currentMonth: Int,
    currentYear: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthNames =
        listOf(
            "January",
            "February",
            "March",
            "April",
            "May",
            "June",
            "July",
            "August",
            "September",
            "October",
            "November",
            "December")

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onPreviousMonth,
            modifier = Modifier.testTag(CalendarScreenTestTags.PREVIOUS_MONTH_BUTTON)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
        }

        Text(
            text = "${monthNames[currentMonth]} $currentYear",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag(CalendarScreenTestTags.MONTH_YEAR_TEXT))

        IconButton(
            onClick = onNextMonth,
            modifier = Modifier.testTag(CalendarScreenTestTags.NEXT_MONTH_BUTTON)) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
        }
    }
}

/**
 * Calendar grid displaying days of the month.
 *
 * @param currentMonth The current month (0-11)
 * @param currentYear The current year
 * @param selectedDate The currently selected date timestamp
 * @param daysWithItems Set of day numbers that have events
 * @param onDateSelected Callback when a date is selected
 */
@Composable
private fun CalendarGrid(
    currentMonth: Int,
    currentYear: Int,
    selectedDate: Long,
    daysWithItems: Set<Int>,
    onDateSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.YEAR, currentYear)
    calendar.set(Calendar.MONTH, currentMonth)
    calendar.set(Calendar.DAY_OF_MONTH, 1)

    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val selectedCalendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
    val selectedDay =
        if (selectedCalendar.get(Calendar.MONTH) == currentMonth &&
            selectedCalendar.get(Calendar.YEAR) == currentYear) {
            selectedCalendar.get(Calendar.DAY_OF_MONTH)
        } else null

    val today = Calendar.getInstance()
    val isCurrentMonth =
        today.get(Calendar.MONTH) == currentMonth && today.get(Calendar.YEAR) == currentYear
    val todayDay = if (isCurrentMonth) today.get(Calendar.DAY_OF_MONTH) else null

    Column(modifier = Modifier.fillMaxWidth().testTag(CalendarScreenTestTags.CALENDAR_GRID)) {
        // Week day headers
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("m", "t", "w", "t", "f", "s", "s").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(Dimens.Spacing.small))

        // Calendar days grid
        var dayCounter = 1
        for (week in 0..5) {
            if (dayCounter > daysInMonth) break

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (dayOfWeek in 0..6) {
                    val shouldShowDay = week > 0 || dayOfWeek >= firstDayOfWeek
                    val day = if (shouldShowDay && dayCounter <= daysInMonth) dayCounter++ else null

                    DayCell(
                        day = day,
                        isSelected = day == selectedDay,
                        isToday = day == todayDay,
                        hasItems = day != null && day in daysWithItems,
                        onDayClick = {
                            day?.let {
                                val timestamp =
                                    Calendar.getInstance()
                                        .apply {
                                            set(Calendar.YEAR, currentYear)
                                            set(Calendar.MONTH, currentMonth)
                                            set(Calendar.DAY_OF_MONTH, it)
                                        }
                                        .timeInMillis
                                onDateSelected(timestamp)
                            }
                        })
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing.small))
        }
    }
}

/**
 * Individual day cell in the calendar grid.
 *
 * @param day The day number (1-31), or null for empty cells
 * @param isSelected Whether this day is currently selected
 * @param isToday Whether this day is today
 * @param hasItems Whether this day has events
 * @param onDayClick Callback when the day is clicked
 */
@Composable
private fun RowScope.DayCell(
    day: Int?,
    isSelected: Boolean,
    isToday: Boolean,
    hasItems: Boolean,
    onDayClick: () -> Unit
) {
    Box(
        modifier =
            Modifier.weight(1f)
                .aspectRatio(1f)
                .then(
                    if (day != null)
                        Modifier.clickable { onDayClick() }
                            .testTag(CalendarScreenTestTags.dayCell(day))
                    else Modifier),
        contentAlignment = Alignment.Center) {
        if (day != null) {
            // Background circle for selected or today
            if (isSelected || isToday) {
                Box(
                    modifier =
                        Modifier.size(Dimens.Profile.photoSmall)
                            .background(
                                color =
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape))
            }

            // Day number
            Text(
                text = day.toString().padStart(2, '0'),
                style = MaterialTheme.typography.bodyMedium,
                color =
                    when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal)

            // Indicator dot for days with events
            if (hasItems && !isSelected) {
                Box(
                    modifier =
                        Modifier.align(Alignment.BottomCenter)
                            .padding(bottom = Dimens.Padding.extraSmall)
                            .size(Dimens.Padding.extraSmall)
                            .background(
                                color = MaterialTheme.colorScheme.primary, shape = CircleShape))
            }
        }
    }
}

/**
 * Empty state message when no events on selected date.
 */
@Composable
private fun EmptyStateMessage() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Spacing.extraLarge)) {
        Text(
            text = "You have no events\non this date.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.fillMaxWidth().testTag(CalendarScreenTestTags.EMPTY_STATE_TEXT))
    }
}

/**
 * List of event items (events and series).
 *
 * @param items List of event items to display
 * @param onSelectEvent Callback invoked when a user taps on an event card
 * @param onSelectSerie Callback invoked when a user taps on a serie card
 */
@Composable
private fun EventsList(
    items: List<EventItem>,
    onSelectEvent: (Event) -> Unit,
    onSelectSerie: (Serie) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.medium)) {
        items(count = items.size, key = { index ->
            when (val item = items[index]) {
                is EventItem.SingleEvent -> "event_${item.event.eventId}"
                is EventItem.EventSerie -> "serie_${item.serie.serieId}"
            }
        }) { index ->
            when (val item = items[index]) {
                is EventItem.SingleEvent -> {
                    EventCard(
                        modifier = Modifier.padding(vertical = Dimens.Spacing.extraSmall),
                        event = item.event,
                        onClick = { onSelectEvent(item.event) },
                        testTag = CalendarScreenTestTags.eventCard(item.event.eventId))
                }
                is EventItem.EventSerie -> {
                    SerieCard(
                        modifier = Modifier.padding(vertical = Dimens.Spacing.extraSmall),
                        serie = item.serie,
                        onClick = { onSelectSerie(item.serie) },
                        testTag = CalendarScreenTestTags.eventCard(item.serie.serieId))
                }
            }
        }
    }
}
