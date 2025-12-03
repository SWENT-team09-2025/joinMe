package com.android.joinme.model.invitation

import com.google.firebase.Timestamp

/** Represents the type of invitation to join a group, event, or serie. */
enum class InvitationType {
  INVITATION_TO_GROUP,
  INVITATION_TO_EVENT,
  INVITATION_TO_SERIES;

  /**
   * Converts a string representation of the invitation type to its corresponding enum value.
   *
   * @param InvitationType The string representation of the invitation type.
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
  fun toDisplayString(): String {
    return when (this) {
      INVITATION_TO_GROUP -> "group"
      INVITATION_TO_EVENT -> "event"
      INVITATION_TO_SERIES -> "serie"
    }
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
