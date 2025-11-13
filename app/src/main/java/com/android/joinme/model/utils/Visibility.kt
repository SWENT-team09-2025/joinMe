package com.android.joinme.model.utils

import java.util.Locale

/**
 * Represents the visibility level of content within the JoinMe application.
 *
 * This enum is used to control who can view events, series, and other content.
 *
 * @property PUBLIC Content is visible and accessible to all users
 * @property PRIVATE Content is only visible to invited participants or the owner
 */
enum class Visibility {
  PUBLIC,
  PRIVATE
}

/**
 * Converts the Visibility enum to a human-readable display string.
 *
 * The enum value is converted to lowercase and the first character is capitalized. For example,
 * "PUBLIC" becomes "Public", "PRIVATE" becomes "Private".
 *
 * @return A formatted string representation of the visibility level suitable for display in UI
 */
fun Visibility.displayString(): String =
    name.lowercase(Locale.ROOT).replaceFirstChar {
      if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
