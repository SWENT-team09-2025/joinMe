package com.android.joinme.model.invitation.deepLink

import android.content.Intent
import android.net.Uri
import com.android.joinme.BuildConfig

/** Service for handling deep links in the application. */
object DeepLinkService {
  private const val BASE_URL = BuildConfig.DEEPLINK_BASE_URL
  private const val INVITATION_PATH = BuildConfig.DEEPLINK_INVITATION_PATH

  /**
   * Generates the invitation link for the given token.
   *
   * @param token The token to be included in the invitation link.
   * @return The generated invitation link.
   */
  fun generateInvitationLink(token: String): String = "$BASE_URL/$INVITATION_PATH/$token"

  /**
   * Parses the invitation link from the intent.
   *
   * @param intent The intent to be parsed.
   * @return The token extracted from the invitation link, or null if the intent does not contain a
   *   valid invitation link.
   */
  fun parseInvitationLink(intent: Intent): String? {
    val uri: Uri = intent.data ?: return null
    val path = uri.path ?: return null

    // Expected format: /invite/{token}
    val segments = path.split("/").filter { it.isNotEmpty() }
    if (segments.size != 2 || segments[0] != INVITATION_PATH) return null

    return segments[1]
  }

  /**
   * Checks if the intent has an invitation link.
   *
   * @param intent The intent to be checked.
   * @return True if the intent has an invitation link, false otherwise.
   */
  fun hasInvitationLink(intent: Intent): Boolean = parseInvitationLink(intent) != null
}
