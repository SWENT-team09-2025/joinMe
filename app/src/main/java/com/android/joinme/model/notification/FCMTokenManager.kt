package com.android.joinme.model.notification

import android.content.Context
import android.util.Log
import com.android.joinme.model.profile.ProfileRepositoryFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Manages Firebase Cloud Messaging tokens for the current user.
 *
 * This object provides methods to initialize, update, and clear FCM tokens in Firestore, enabling
 * push notifications for the user.
 */
object FCMTokenManager {

  private const val TAG = "FCMTokenManager"

  /**
   * Initializes and stores the FCM token for the currently authenticated user.
   *
   * This method retrieves the device's FCM token and stores it in the user's Firestore profile.
   * Should be called when the user logs in or when the app starts with an authenticated user.
   *
   * @param context The application context
   */
  fun initializeFCMToken(context: Context) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
      Log.w(TAG, "Cannot initialize FCM token: no user is logged in")
      return
    }

    CoroutineScope(Dispatchers.IO).launch {
      try {
        val token = FirebaseMessaging.getInstance().token.await()
        Log.d(TAG, "FCM Token retrieved: $token")

        // Update the user's profile with the FCM token
        val db = FirebaseFirestore.getInstance()
        db.collection("profiles")
            .document(currentUser.uid)
            .update("fcmToken", token)
            .await()

        Log.d(TAG, "FCM token saved to Firestore for user ${currentUser.uid}")
      } catch (e: Exception) {
        Log.e(TAG, "Error initializing FCM token", e)
      }
    }
  }

  /**
   * Updates the FCM token for the currently authenticated user.
   *
   * This method should be called when the FCM token is refreshed by Firebase.
   *
   * @param newToken The new FCM token
   */
  fun updateFCMToken(newToken: String) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
      Log.w(TAG, "Cannot update FCM token: no user is logged in")
      return
    }

    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = FirebaseFirestore.getInstance()
        db.collection("profiles")
            .document(currentUser.uid)
            .update("fcmToken", newToken)
            .await()

        Log.d(TAG, "FCM token updated in Firestore for user ${currentUser.uid}")
      } catch (e: Exception) {
        Log.e(TAG, "Error updating FCM token", e)
      }
    }
  }

  /**
   * Clears the FCM token for the currently authenticated user.
   *
   * This method should be called when the user logs out to ensure they no longer receive
   * notifications on this device.
   */
  fun clearFCMToken() {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
      Log.w(TAG, "Cannot clear FCM token: no user is logged in")
      return
    }

    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = FirebaseFirestore.getInstance()
        db.collection("profiles").document(currentUser.uid).update("fcmToken", null).await()

        Log.d(TAG, "FCM token cleared from Firestore for user ${currentUser.uid}")
      } catch (e: Exception) {
        Log.e(TAG, "Error clearing FCM token", e)
      }
    }
  }
}
