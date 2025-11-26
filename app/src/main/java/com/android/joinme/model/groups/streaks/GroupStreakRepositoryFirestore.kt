package com.android.joinme.model.groups.streaks

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Firestore implementation of [GroupStreakRepository].
 *
 * Stores streak documents at: groups/{groupId}/streaks/{userId}
 *
 * @property db The FirebaseFirestore instance used for database operations.
 */
class GroupStreakRepositoryFirestore(private val db: FirebaseFirestore) : GroupStreakRepository {

  companion object {
    private const val TAG = "GroupStreakRepoFirestore"
    private const val GROUPS_COLLECTION = "groups"
    private const val STREAKS_SUBCOLLECTION = "streaks"
  }

  override suspend fun getStreaksForGroup(groupId: String): List<GroupStreak> {
    val snapshot =
        db.collection(GROUPS_COLLECTION)
            .document(groupId)
            .collection(STREAKS_SUBCOLLECTION)
            .get()
            .await() ?: throw Exception("$TAG: Failed to fetch streaks for group $groupId")

    return snapshot.documents.mapNotNull { doc -> documentToGroupStreak(doc, groupId) }
  }

  override suspend fun getStreakForUser(groupId: String, userId: String): GroupStreak? {
    val doc =
        db.collection(GROUPS_COLLECTION)
            .document(groupId)
            .collection(STREAKS_SUBCOLLECTION)
            .document(userId)
            .get()
            .await()
            ?: throw Exception("$TAG: Failed to fetch streak for user $userId in group $groupId")

    return if (doc.exists()) documentToGroupStreak(doc, groupId) else null
  }

  override suspend fun updateStreak(groupId: String, userId: String, streak: GroupStreak) {
    val data = groupStreakToMap(streak)

    db.collection(GROUPS_COLLECTION)
        .document(groupId)
        .collection(STREAKS_SUBCOLLECTION)
        .document(userId)
        .set(data, SetOptions.merge())
        .await()
        ?: throw Exception("$TAG: Failed to update streak for user $userId in group $groupId")
  }

  /**
   * Converts a Firestore DocumentSnapshot to a GroupStreak object.
   *
   * @param doc The DocumentSnapshot to convert.
   * @param groupId The group ID (used since it's not stored in the document).
   * @return The converted GroupStreak, or null if conversion fails.
   */
  private fun documentToGroupStreak(doc: DocumentSnapshot, groupId: String): GroupStreak? {
    return try {
      GroupStreak(
          groupId = groupId,
          userId = doc.id,
          currentStreakWeeks = doc.getLong("currentStreakWeeks")?.toInt() ?: 0,
          currentStreakActivities = doc.getLong("currentStreakActivities")?.toInt() ?: 0,
          currentStreakStartDate = doc.getTimestamp("currentStreakStartDate"),
          lastActiveWeekStart = doc.getTimestamp("lastActiveWeekStart"),
          bestStreakWeeks = doc.getLong("bestStreakWeeks")?.toInt() ?: 0,
          bestStreakActivities = doc.getLong("bestStreakActivities")?.toInt() ?: 0)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to convert document ${doc.id} to GroupStreak", e)
      null
    }
  }

  /**
   * Converts a GroupStreak object to a Map for Firestore storage.
   *
   * Note: groupId and userId are not stored in the document as they are derived from the document
   * path (groups/{groupId}/streaks/{userId}).
   *
   * @param streak The GroupStreak to convert.
   * @return A Map representing the streak data.
   */
  private fun groupStreakToMap(streak: GroupStreak): Map<String, Any?> {
    return mapOf(
        "currentStreakWeeks" to streak.currentStreakWeeks,
        "currentStreakActivities" to streak.currentStreakActivities,
        "currentStreakStartDate" to streak.currentStreakStartDate,
        "lastActiveWeekStart" to streak.lastActiveWeekStart,
        "bestStreakWeeks" to streak.bestStreakWeeks,
        "bestStreakActivities" to streak.bestStreakActivities)
  }
}
