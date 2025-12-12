package com.android.joinme.ui.chat

import android.content.Context
import com.android.joinme.R
import com.android.joinme.model.chat.ChatListItem
import com.android.joinme.model.chat.Message
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Formats a timestamp into a human-readable date string for date headers.
 *
 * Returns different formats based on how recent the date is:
 * - "Today" for today's date
 * - "Yesterday" for yesterday
 * - Day name (e.g., "Monday") for dates within the current week
 * - "Month Day" (e.g., "January 15") for dates in the current year
 * - "Month Day, Year" (e.g., "January 15, 2024") for dates in previous years
 *
 * @param context Android context for accessing string resources
 * @param timestamp The timestamp in milliseconds to format
 * @return A formatted date string appropriate for the date header
 */
fun formatDateHeader(context: Context, timestamp: Long): String {
  val messageCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
  val todayCalendar = Calendar.getInstance()

  // Check if it's today
  if (isSameDay(messageCalendar, todayCalendar)) {
    return context.getString(R.string.date_header_today)
  }

  // Check if it's yesterday
  val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
  if (isSameDay(messageCalendar, yesterdayCalendar)) {
    return context.getString(R.string.date_header_yesterday)
  }

  // Check if it's within the current week (and after yesterday)
  val weekAgoCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
  if (messageCalendar.after(weekAgoCalendar) && messageCalendar.before(todayCalendar)) {
    // Return day name (e.g., "Monday")
    val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    return dayFormat.format(Date(timestamp))
  }

  // Check if it's in the current year
  if (messageCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR)) {
    // Return "Month Day" (e.g., "January 15")
    val monthDayFormat = SimpleDateFormat("MMMM d", Locale.getDefault())
    return monthDayFormat.format(Date(timestamp))
  }

  // For previous years, return "Month Day, Year" (e.g., "January 15, 2024")
  val fullDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
  return fullDateFormat.format(Date(timestamp))
}

/**
 * Checks if two Calendar instances represent the same day.
 *
 * @param cal1 First calendar to compare
 * @param cal2 Second calendar to compare
 * @return true if both calendars represent the same day, false otherwise
 */
private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
  return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
      cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

/**
 * Groups messages by date and inserts date headers between messages from different days.
 *
 * This function takes a sorted list of messages (oldest to newest) and returns a list of
 * ChatListItems that includes both messages and date headers. A date header is inserted whenever
 * the date changes between consecutive messages.
 *
 * Example:
 * ```
 * Input:  [msg1(Jan 14), msg2(Jan 14), msg3(Jan 15), msg4(Jan 15)]
 * Output: [DateHeader(Jan 14), msg1, msg2, DateHeader(Jan 15), msg3, msg4]
 * ```
 *
 * @param messages List of messages sorted by timestamp (oldest first)
 * @return List of ChatListItems with date headers inserted
 */
fun groupMessagesByDate(messages: List<Message>): List<ChatListItem> {
  if (messages.isEmpty()) {
    return emptyList()
  }

  val result = mutableListOf<ChatListItem>()
  var currentDate: Calendar? = null

  messages.forEach { message ->
    val messageDate = Calendar.getInstance().apply { timeInMillis = message.timestamp }

    // Check if we need to insert a date header
    if (currentDate == null || !isSameDay(currentDate!!, messageDate)) {
      // Insert date header for the new day
      result.add(ChatListItem.DateHeader(getStartOfDay(message.timestamp)))
      currentDate = messageDate
    }

    // Add the message
    result.add(ChatListItem.MessageItem(message))
  }

  return result
}

/**
 * Gets the timestamp for the start of the day (00:00:00) for a given timestamp.
 *
 * This ensures all messages from the same day share the same date header timestamp.
 *
 * @param timestamp Any timestamp during the day
 * @return Timestamp representing the start of that day (00:00:00)
 */
private fun getStartOfDay(timestamp: Long): Long {
  val calendar =
      Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
  return calendar.timeInMillis
}
