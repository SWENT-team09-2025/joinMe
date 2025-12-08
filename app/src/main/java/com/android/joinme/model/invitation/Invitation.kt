package com.android.joinme.model.invitation

import com.google.firebase.Timestamp
import java.util.Locale

/** Represents the type of invitation to join a group, event, or serie. */
@Suppress("java:S115")
enum class InvitationType {
  GROUP,
  EVENT,
  SERIE;

  /**
   * Converts a string representation of the invitation type to its corresponding enum value.
   *
   * @param String The InvitationType of the string.
   */
  companion object {
    fun fromString(value: String): InvitationType? {
      return values().find { it.name == value.uppercase() }
    }
  }

  /**
   * Converts the invitation type to a human-readable string representation.
   *
   * @return A string representation of the invitation type.
   */
  fun toDisplayString(): String =
      name.lowercase(Locale.ROOT).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
      }
}

/** Represents an invitation to join a group, event, or serie. */
data class Invitation(
    val token: String,
    val type: InvitationType,
    val targetId: String,
    val createdBy: String,
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp? = null
) {
  /**
   * Checks if the invitation is still valid.
   *
   * @return `true` if the invitation is still valid, `false` otherwise.
   */
  fun isValid(): Boolean {
    return expiresAt?.toDate()?.after(Timestamp.now().toDate()) ?: true
  }
}
