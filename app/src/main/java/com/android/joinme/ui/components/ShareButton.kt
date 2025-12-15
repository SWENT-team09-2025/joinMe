package com.android.joinme.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.android.joinme.R
import com.android.joinme.model.invitation.InvitationRepositoryProvider
import com.android.joinme.model.invitation.InvitationType
import com.android.joinme.model.invitation.deepLink.DeepLinkService
import com.android.joinme.ui.theme.Dimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val SHARE_SHEET_TYPE = "text/plain"
const val SEVEN_DAYS = 7.0

/** Test tags for ShareButton UI elements. */
object ShareButtonTestTags {
  const val SHARE_BUTTON = "shareButton"
}
/**
 * IconButton that creates an invitation link and opens Android Share Sheet. Use this in TopAppBar
 * for Event/Serie screens.
 *
 * @param invitationType Type of invitation (GROUP, EVENT, or SERIES)
 * @param targetId ID of the target entity
 * @param createdBy User ID creating the invitation
 * @param expiresInDays Expiration in days (default: 7, null = no expiration)
 * @param onError Error callback
 * @param modifier Modifier for the button
 */
@Composable
fun ShareButton(
    modifier: Modifier = Modifier,
    invitationType: InvitationType,
    targetId: String,
    createdBy: String,
    expiresInDays: Double? = SEVEN_DAYS,
    onError: (String) -> Unit = {}
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  IconButton(
      onClick = {
        coroutineScope.launch {
          shareInvitation(invitationType, targetId, createdBy, expiresInDays, context, onError)
        }
      },
      modifier = modifier.testTag(ShareButtonTestTags.SHARE_BUTTON)) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = stringResource(R.string.share_invitation_link),
            modifier = Modifier.size(Dimens.IconSize.medium))
      }
}
/**
 * Helper function that creates an invitation and opens the Android Share Sheet. This is the
 * centralized logic used across the app for sharing invitations.
 *
 * @param invitationType Type of invitation (GROUP, EVENT, or SERIES)
 * @param targetId ID of the target entity
 * @param createdBy User ID creating the invitation
 * @param expiresInDays Expiration in days (default: 7, null = no expiration)
 * @param context Android context
 * @param onError Error callback
 */
suspend fun shareInvitation(
    invitationType: InvitationType,
    targetId: String,
    createdBy: String,
    expiresInDays: Double? = SEVEN_DAYS,
    context: Context,
    onError: (String) -> Unit = {}
) {
  try {

    val invitationRepository = InvitationRepositoryProvider.repository

    val result =
        invitationRepository.createInvitation(
            type = invitationType,
            targetId = targetId,
            createdBy = createdBy,
            expiresInDays = expiresInDays)

    result
        .onSuccess { token ->
          val link = DeepLinkService.generateInvitationLink(token)
          openShareSheet(context = context, link = link, type = invitationType)
        }
        .onFailure { e ->
          withContext(Dispatchers.Main) {
            val errorMsg = "Failed to create invitation: ${e.message}"
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            onError(errorMsg)
          }
        }
  } catch (e: Exception) {
    withContext(Dispatchers.Main) {
      val errorMsg = "Failed to create invitation: ${e.message}"
      Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
      onError(errorMsg)
    }
  }
}

/**
 * Opens the Android Share Sheet with the invitation link.
 *
 * @param context Android context
 * @param link The invitation link to share
 * @param type Type of invitation for the share message
 */
private fun openShareSheet(context: Context, link: String, type: InvitationType) {
  val shareIntent =
      Intent().apply {
        action = Intent.ACTION_SEND
        setType(SHARE_SHEET_TYPE)
        val message =
            context.getString(R.string.message_invitation_link, type.toDisplayString(), link)
        putExtra(Intent.EXTRA_TEXT, message)
      }

  val chooserIntent =
      Intent.createChooser(shareIntent, context.getString(R.string.share_invitation_link))
  context.startActivity(chooserIntent)
}
