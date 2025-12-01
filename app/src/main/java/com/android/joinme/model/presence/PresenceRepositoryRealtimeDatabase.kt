package com.android.joinme.model.presence

// Implemented with help of Claude AI

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Realtime Database implementation of [PresenceRepository].
 *
 * This implementation uses Firebase Realtime Database's special presence features:
 * - `onDisconnect()` handlers to automatically update presence when users disconnect
 * - Server timestamps for accurate lastSeen tracking
 * - Dual structure for optimized queries
 *
 * Database structure:
 * ```
 * presence/
 *   {contextId}/
 *     {userId}/
 *       visitorId: "..."
 *       online: true/false
 *       lastSeen: <server timestamp>
 *
 * userContexts/
 *   {userId}/
 *     {contextId}: true
 * ```
 *
 * The `presence/` structure is used for observing online users in a context. The `userContexts/`
 * structure is an index for quickly finding which contexts a user is in.
 */
class PresenceRepositoryRealtimeDatabase(private val database: FirebaseDatabase) :
    PresenceRepository {

  companion object {
    private const val TAG = "PresenceRepositoryRTDB"
    private const val PRESENCE_PATH = "presence"
    private const val USER_CONTEXTS_PATH = "userContexts"
    private const val FIELD_USER_ID = "visitorId"
    private const val FIELD_ONLINE = "online"
    private const val FIELD_LAST_SEEN = "lastSeen"
  }

  private val presenceRef: DatabaseReference = database.getReference(PRESENCE_PATH)
  private val userContextsRef: DatabaseReference = database.getReference(USER_CONTEXTS_PATH)

  override suspend fun setUserOnline(userId: String, contextIds: List<String>) {
    if (userId.isBlank()) {
      Log.w(TAG, "setUserOnline called with blank userId")
      return
    }

    for (contextId in contextIds) {
      if (contextId.isBlank()) {
        Log.w(TAG, "Skipping blank contextId")
        continue
      }
      try {
        val userPresenceRef = presenceRef.child(contextId).child(userId)

        // Set up onDisconnect handler first - this will trigger when user closes app
        userPresenceRef.child(FIELD_ONLINE).onDisconnect().setValue(false).await()
        userPresenceRef
            .child(FIELD_LAST_SEEN)
            .onDisconnect()
            .setValue(ServerValue.TIMESTAMP)
            .await()

        // Set user as online with current data
        val presenceData =
            mapOf(
                FIELD_USER_ID to userId,
                FIELD_ONLINE to true,
                FIELD_LAST_SEEN to ServerValue.TIMESTAMP)

        userPresenceRef.setValue(presenceData).await()

        // Update userContexts index for fast lookup during setUserOffline
        userContextsRef.child(userId).child(contextId).setValue(true).await()
      } catch (e: Exception) {
        Log.e(TAG, "Failed to set user online in context $contextId", e)
      }
    }
  }

  override suspend fun setUserOffline(userId: String) {
    if (userId.isBlank()) {
      Log.w(TAG, "setUserOffline called with blank userId")
      return
    }

    try {
      // Get only this user's contexts from the index (optimized - doesn't load all contexts)
      val userContextsSnapshot = userContextsRef.child(userId).get().await()

      for (contextSnapshot in userContextsSnapshot.children) {
        val contextId = contextSnapshot.key ?: continue
        try {
          val userPresenceRef = presenceRef.child(contextId).child(userId)

          // Cancel any existing onDisconnect handlers
          userPresenceRef.onDisconnect().cancel().await()

          // Update presence to offline
          val updates = mapOf(FIELD_ONLINE to false, FIELD_LAST_SEEN to ServerValue.TIMESTAMP)
          userPresenceRef.updateChildren(updates).await()
        } catch (e: Exception) {
          Log.e(TAG, "Failed to set user offline in context $contextId", e)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to set user offline globally: $userId", e)
    }
  }

  override fun observeOnlineUsersCount(contextId: String, currentUserId: String): Flow<Int> =
      callbackFlow {
        if (contextId.isBlank() || currentUserId.isBlank()) {
          Log.w(TAG, "observeOnlineUsersCount called with blank contextId or currentUserId")
          trySend(0)
          close()
          return@callbackFlow
        }

        val contextPresenceRef = presenceRef.child(contextId)

        val listener =
            object : ValueEventListener {
              override fun onDataChange(snapshot: DataSnapshot) {
                var count = 0

                for (userSnapshot in snapshot.children) {
                  val visitorId = userSnapshot.key ?: continue
                  if (visitorId == currentUserId) continue

                  val isOnline =
                      userSnapshot.child(FIELD_ONLINE).getValue(Boolean::class.java) ?: false
                  if (isOnline) {
                    count++
                  }
                }

                trySend(count)
              }

              override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error observing online users count", error.toException())
                trySend(0)
              }
            }

        contextPresenceRef.addValueEventListener(listener)

        awaitClose { contextPresenceRef.removeEventListener(listener) }
      }

  override fun observeOnlineUserIds(contextId: String, currentUserId: String): Flow<List<String>> =
      callbackFlow {
        if (contextId.isBlank() || currentUserId.isBlank()) {
          Log.w(TAG, "observeOnlineUserIds called with blank contextId or currentUserId")
          trySend(emptyList())
          close()
          return@callbackFlow
        }

        val contextPresenceRef = presenceRef.child(contextId)

        val listener =
            object : ValueEventListener {
              override fun onDataChange(snapshot: DataSnapshot) {
                val onlineUserIds = mutableListOf<String>()

                for (userSnapshot in snapshot.children) {
                  val visitorId = userSnapshot.key ?: continue
                  if (visitorId == currentUserId) continue

                  val isOnline =
                      userSnapshot.child(FIELD_ONLINE).getValue(Boolean::class.java) ?: false
                  if (isOnline) {
                    onlineUserIds.add(visitorId)
                  }
                }

                trySend(onlineUserIds)
              }

              override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error observing online user IDs", error.toException())
                trySend(emptyList())
              }
            }

        contextPresenceRef.addValueEventListener(listener)

        awaitClose { contextPresenceRef.removeEventListener(listener) }
      }

  override suspend fun cleanupStalePresence(staleThresholdMs: Long) {
    try {
      val snapshot = presenceRef.get().await()
      val currentTime = System.currentTimeMillis()

      for (contextSnapshot in snapshot.children) {
        val contextId = contextSnapshot.key ?: continue

        for (userSnapshot in contextSnapshot.children) {
          val userId = userSnapshot.key ?: continue
          val lastSeen = userSnapshot.child(FIELD_LAST_SEEN).getValue(Long::class.java) ?: continue
          val isOnline = userSnapshot.child(FIELD_ONLINE).getValue(Boolean::class.java) ?: false

          // If user is marked as online but hasn't been seen recently, mark them offline
          if (isOnline && currentTime - lastSeen > staleThresholdMs) {
            presenceRef.child(contextId).child(userId).child(FIELD_ONLINE).setValue(false).await()
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to cleanup stale presence", e)
    }
  }
}
