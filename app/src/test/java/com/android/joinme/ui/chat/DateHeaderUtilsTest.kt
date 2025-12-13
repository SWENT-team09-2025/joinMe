package com.android.joinme.ui.chat

// Implemented with help of Claude AI
import android.content.Context
import com.android.joinme.R
import com.android.joinme.model.chat.ChatListItem
import com.android.joinme.model.chat.Message
import com.android.joinme.model.chat.MessageType
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

/** Tests for date header formatting and message grouping utilities. */
@RunWith(RobolectricTestRunner::class)
class DateHeaderUtilsTest {

  @Mock private lateinit var mockContext: Context

  @Before
  fun setup() {
    MockitoAnnotations.openMocks(this)

    // Mock string resources
    `when`(mockContext.getString(R.string.date_header_today)).thenReturn("Today")
    `when`(mockContext.getString(R.string.date_header_yesterday)).thenReturn("Yesterday")
  }

  // ============================================================================
  // formatDateHeader Tests
  // ============================================================================

  @Test
  fun formatDateHeader_recentDates_returnsCorrectFormat() {
    // Test today
    val now = System.currentTimeMillis()
    assertEquals("Today", formatDateHeader(mockContext, now))

    // Test yesterday
    val yesterday =
        Calendar.getInstance()
            .apply {
              add(Calendar.DAY_OF_YEAR, -1)
              set(Calendar.HOUR_OF_DAY, 12)
            }
            .timeInMillis
    assertEquals("Yesterday", formatDateHeader(mockContext, yesterday))

    // Test this week (3 days ago) - should return day name
    val thisWeek =
        Calendar.getInstance()
            .apply {
              add(Calendar.DAY_OF_YEAR, -3)
              set(Calendar.HOUR_OF_DAY, 12)
            }
            .timeInMillis
    val weekResult = formatDateHeader(mockContext, thisWeek)
    assertTrue(
        weekResult.matches(
            Regex(
                "Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday",
                RegexOption.IGNORE_CASE)))
  }

  @Test
  fun formatDateHeader_olderDates_returnsCorrectFormat() {
    // Test this year (30 days ago) - should return "Month Day"
    val thisYear =
        Calendar.getInstance()
            .apply {
              add(Calendar.DAY_OF_YEAR, -30)
              set(Calendar.HOUR_OF_DAY, 12)
            }
            .timeInMillis
    val thisYearResult = formatDateHeader(mockContext, thisYear)
    assertTrue(thisYearResult.matches(Regex("[A-Z][a-z]+ \\d{1,2}")))

    // Test previous year - should return "Month Day, Year"
    val lastYear =
        Calendar.getInstance()
            .apply {
              add(Calendar.YEAR, -1)
              set(Calendar.MONTH, Calendar.JUNE)
              set(Calendar.DAY_OF_MONTH, 15)
              set(Calendar.HOUR_OF_DAY, 12)
            }
            .timeInMillis
    val lastYearResult = formatDateHeader(mockContext, lastYear)
    assertTrue(lastYearResult.matches(Regex("[A-Z][a-z]+ \\d{1,2}, \\d{4}")))
  }

  // ============================================================================
  // groupMessagesByDate Tests
  // ============================================================================

  @Test
  fun groupMessagesByDate_emptyAndSingleMessage_handledCorrectly() {
    // Test empty list
    val emptyResult = groupMessagesByDate(emptyList())
    assertTrue(emptyResult.isEmpty())

    // Test single message
    val message = createMessage("1", timestamp = getTodayTimestamp())
    val singleResult = groupMessagesByDate(listOf(message))
    assertEquals(2, singleResult.size)
    assertTrue(singleResult[0] is ChatListItem.DateHeader)
    assertTrue(singleResult[1] is ChatListItem.MessageItem)
    assertEquals(message, (singleResult[1] as ChatListItem.MessageItem).message)
  }

  @Test
  fun groupMessagesByDate_sameDayMessages_insertsSingleHeader() {
    val today = getTodayTimestamp()
    val msg1 = createMessage("1", timestamp = today)
    val msg2 = createMessage("2", timestamp = today + 1000) // 1 second later
    val msg3 = createMessage("3", timestamp = today + 2000) // 2 seconds later

    val result = groupMessagesByDate(listOf(msg1, msg2, msg3))

    // Should be: [DateHeader, msg1, msg2, msg3]
    assertEquals(4, result.size)
    assertTrue(result[0] is ChatListItem.DateHeader)
    assertEquals("1", (result[1] as ChatListItem.MessageItem).message.id)
    assertEquals("2", (result[2] as ChatListItem.MessageItem).message.id)
    assertEquals("3", (result[3] as ChatListItem.MessageItem).message.id)
  }

  @Test
  fun groupMessagesByDate_multipleDays_insertsHeadersCorrectly() {
    val today = getTodayTimestamp()
    val yesterday = getYesterdayTimestamp()
    val twoDaysAgo = getDaysAgoTimestamp(2)

    val msg1 = createMessage("1", timestamp = twoDaysAgo)
    val msg2 = createMessage("2", timestamp = yesterday)
    val msg3 = createMessage("3", timestamp = yesterday + 3600000) // 1 hour later
    val msg4 = createMessage("4", timestamp = today)

    val result = groupMessagesByDate(listOf(msg1, msg2, msg3, msg4))

    // Should be: [Header(2 days ago), msg1, Header(yesterday), msg2, msg3, Header(today), msg4]
    assertEquals(7, result.size)

    // Verify headers at correct positions
    assertTrue(result[0] is ChatListItem.DateHeader)
    assertTrue(result[2] is ChatListItem.DateHeader)
    assertTrue(result[5] is ChatListItem.DateHeader)

    // Verify messages are in correct order
    assertEquals("1", (result[1] as ChatListItem.MessageItem).message.id)
    assertEquals("2", (result[3] as ChatListItem.MessageItem).message.id)
    assertEquals("3", (result[4] as ChatListItem.MessageItem).message.id)
    assertEquals("4", (result[6] as ChatListItem.MessageItem).message.id)
  }

  @Test
  fun groupMessagesByDate_midnightBoundary_createsNewHeader() {
    // Message just before midnight
    val beforeMidnight =
        Calendar.getInstance()
            .apply {
              set(Calendar.HOUR_OF_DAY, 23)
              set(Calendar.MINUTE, 59)
              set(Calendar.SECOND, 59)
            }
            .timeInMillis

    // Message just after midnight (next day)
    val afterMidnight =
        Calendar.getInstance()
            .apply {
              add(Calendar.DAY_OF_YEAR, 1)
              set(Calendar.HOUR_OF_DAY, 0)
              set(Calendar.MINUTE, 0)
              set(Calendar.SECOND, 1)
            }
            .timeInMillis

    val msg1 = createMessage("1", timestamp = beforeMidnight)
    val msg2 = createMessage("2", timestamp = afterMidnight)

    val result = groupMessagesByDate(listOf(msg1, msg2))

    // Should have 2 headers (different days) and 2 messages
    assertEquals(4, result.size)
    assertTrue(result[0] is ChatListItem.DateHeader)
    assertTrue(result[1] is ChatListItem.MessageItem)
    assertTrue(result[2] is ChatListItem.DateHeader)
    assertTrue(result[3] is ChatListItem.MessageItem)
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

  private fun createMessage(
      id: String,
      timestamp: Long = System.currentTimeMillis(),
      senderId: String = "user1",
      senderName: String = "User 1",
      content: String = "Test message",
      type: MessageType = MessageType.TEXT
  ): Message {
    return Message(
        id = id,
        conversationId = "conv1",
        senderId = senderId,
        senderName = senderName,
        content = content,
        timestamp = timestamp,
        type = type,
        readBy = emptyList(),
        isEdited = false)
  }

  private fun getTodayTimestamp(): Long {
    return Calendar.getInstance()
        .apply {
          set(Calendar.HOUR_OF_DAY, 12)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
        .timeInMillis
  }

  private fun getYesterdayTimestamp(): Long {
    return Calendar.getInstance()
        .apply {
          add(Calendar.DAY_OF_YEAR, -1)
          set(Calendar.HOUR_OF_DAY, 12)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
        .timeInMillis
  }

  private fun getDaysAgoTimestamp(days: Int): Long {
    return Calendar.getInstance()
        .apply {
          add(Calendar.DAY_OF_YEAR, -days)
          set(Calendar.HOUR_OF_DAY, 12)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
        .timeInMillis
  }
}
