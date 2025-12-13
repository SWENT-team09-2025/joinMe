package com.android.joinme.model.chat

/**
 * Sealed class representing items that can appear in a chat message list.
 *
 * This allows the chat UI to display both messages and date headers in a unified list, enabling
 * better visual separation of messages by date.
 */
sealed class ChatListItem {
  /**
   * A message item in the chat list.
   *
   * @property message The actual message data
   */
  data class MessageItem(val message: Message) : ChatListItem()

  /**
   * A date header item showing a date separator in the chat list.
   *
   * This appears as a centered chip or divider showing when messages transition from one day to
   * another (e.g., "Today", "Yesterday", "January 15").
   *
   * @property timestamp The timestamp (in milliseconds) representing the start of this day
   */
  data class DateHeader(val timestamp: Long) : ChatListItem()
}
