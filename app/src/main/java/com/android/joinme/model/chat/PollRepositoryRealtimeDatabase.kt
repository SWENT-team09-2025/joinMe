package com.android.joinme.model.chat

// Implemented with help of Claude AI

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Realtime Database implementation of [PollRepository].
 *
 * Polls are stored in the following structure:
 * ```
 * conversations/
 *   {conversationId}/
 *     polls/
 *       {pollId}/
 *         creatorId: "..."
 *         creatorName: "..."
 *         question: "..."
 *         options: [
 *           { id: 0, text: "Option 1", voterIds: ["user1", "user2"] },
 *           { id: 1, text: "Option 2", voterIds: ["user3"] }
 *         ]
 *         isAnonymous: false
 *         allowMultipleAnswers: false
 *         isClosed: false
 *         createdAt: 123456789
 *         closedAt: null
 * ```
 *
 * This provides real-time synchronization where all clients receive updates instantly when polls
 * are created, votes are cast, or polls are modified.
 */
class PollRepositoryRealtimeDatabase(database: FirebaseDatabase) : PollRepository {

  companion object {
    private const val TAG = "PollRepositoryRTDB"
    private const val CONVERSATIONS_PATH = "conversations"
    private const val POLLS_PATH = "polls"

    // Poll field names
    private const val FIELD_CREATOR_ID = "creatorId"
    private const val FIELD_CREATOR_NAME = "creatorName"
    private const val FIELD_QUESTION = "question"
    private const val FIELD_OPTIONS = "options"
    private const val FIELD_IS_ANONYMOUS = "isAnonymous"
    private const val FIELD_ALLOW_MULTIPLE_ANSWERS = "allowMultipleAnswers"
    private const val FIELD_IS_CLOSED = "isClosed"
    private const val FIELD_CREATED_AT = "createdAt"
    private const val FIELD_CLOSED_AT = "closedAt"

    // Option field names
    private const val FIELD_OPTION_ID = "id"
    private const val FIELD_OPTION_TEXT = "text"
    private const val FIELD_OPTION_VOTER_IDS = "voterIds"

    // Type indicators for deserializing from Realtime Database
    private val STRING_LIST_TYPE_INDICATOR =
        object : com.google.firebase.database.GenericTypeIndicator<List<String>>() {}
    private val OPTIONS_LIST_TYPE_INDICATOR =
        object : com.google.firebase.database.GenericTypeIndicator<List<Map<String, Any?>>>() {}
  }

  private val conversationsRef: DatabaseReference = database.getReference(CONVERSATIONS_PATH)

  override fun getNewPollId(): String {
    return conversationsRef.push().key
        ?: throw IllegalStateException("Failed to generate poll ID from Firebase")
  }

  override fun observePollsForConversation(conversationId: String): Flow<List<Poll>> =
      callbackFlow {
        val pollsRef =
            conversationsRef.child(conversationId).child(POLLS_PATH).orderByChild(FIELD_CREATED_AT)

        val listener =
            object : ValueEventListener {
              override fun onDataChange(snapshot: DataSnapshot) {
                val polls = mutableListOf<Poll>()

                for (pollSnapshot in snapshot.children) {
                  val poll = dataSnapshotToPoll(pollSnapshot, conversationId)
                  if (poll != null) {
                    polls.add(poll)
                  } else {
                    Log.w(TAG, "Failed to parse poll snapshot: ${pollSnapshot.key}")
                  }
                }

                trySend(polls)
              }

              override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error observing polls", error.toException())
                trySend(emptyList())
              }
            }

        pollsRef.addValueEventListener(listener)

        awaitClose { pollsRef.removeEventListener(listener) }
      }

  override suspend fun getPoll(conversationId: String, pollId: String): Poll? {
    return try {
      val snapshot =
          conversationsRef.child(conversationId).child(POLLS_PATH).child(pollId).get().await()

      dataSnapshotToPoll(snapshot, conversationId)
    } catch (e: Exception) {
      Log.e(TAG, "Error getting poll: $pollId", e)
      null
    }
  }

  override suspend fun createPoll(poll: Poll) {
    val pollRef = conversationsRef.child(poll.conversationId).child(POLLS_PATH).child(poll.id)

    pollRef.setValue(pollToMap(poll)).await()
  }

  override suspend fun vote(conversationId: String, pollId: String, optionId: Int, userId: String) {
    val pollRef = conversationsRef.child(conversationId).child(POLLS_PATH).child(pollId)

    // Get current poll state
    val snapshot = pollRef.get().await()
    val poll =
        dataSnapshotToPoll(snapshot, conversationId) ?: throw Exception("Poll not found: $pollId")

    if (poll.isClosed) {
      throw Exception("Cannot vote on closed poll")
    }

    // Build updated options
    val updatedOptions =
        poll.options.map { option ->
          if (!poll.allowMultipleAnswers && option.id != optionId && userId in option.voterIds) {
            // For single-answer polls, remove vote from other options
            option.copy(voterIds = option.voterIds.filter { it != userId })
          } else if (option.id == optionId && userId !in option.voterIds) {
            // Add vote to selected option
            option.copy(voterIds = option.voterIds + userId)
          } else {
            option
          }
        }

    // Update options in database
    pollRef.child(FIELD_OPTIONS).setValue(optionsToList(updatedOptions)).await()
  }

  override suspend fun removeVote(
      conversationId: String,
      pollId: String,
      optionId: Int,
      userId: String
  ) {
    val pollRef = conversationsRef.child(conversationId).child(POLLS_PATH).child(pollId)

    // Get current poll state
    val snapshot = pollRef.get().await()
    val poll =
        dataSnapshotToPoll(snapshot, conversationId) ?: throw Exception("Poll not found: $pollId")

    if (poll.isClosed) {
      throw Exception("Cannot modify vote on closed poll")
    }

    // Build updated options
    val updatedOptions =
        poll.options.map { option ->
          if (option.id == optionId && userId in option.voterIds) {
            option.copy(voterIds = option.voterIds.filter { it != userId })
          } else {
            option
          }
        }

    // Update options in database
    pollRef.child(FIELD_OPTIONS).setValue(optionsToList(updatedOptions)).await()
  }

  override suspend fun closePoll(conversationId: String, pollId: String, userId: String) {
    val pollRef = conversationsRef.child(conversationId).child(POLLS_PATH).child(pollId)

    // Verify ownership
    val snapshot = pollRef.get().await()
    val poll =
        dataSnapshotToPoll(snapshot, conversationId) ?: throw Exception("Poll not found: $pollId")

    if (poll.creatorId != userId) {
      throw Exception("Only the poll owner can close this poll")
    }

    val updates = mapOf(FIELD_IS_CLOSED to true, FIELD_CLOSED_AT to System.currentTimeMillis())

    pollRef.updateChildren(updates).await()
  }

  override suspend fun reopenPoll(conversationId: String, pollId: String, userId: String) {
    val pollRef = conversationsRef.child(conversationId).child(POLLS_PATH).child(pollId)

    // Verify ownership
    val snapshot = pollRef.get().await()
    val poll =
        dataSnapshotToPoll(snapshot, conversationId) ?: throw Exception("Poll not found: $pollId")

    if (poll.creatorId != userId) {
      throw Exception("Only the poll owner can reopen this poll")
    }

    val updates = mapOf(FIELD_IS_CLOSED to false, FIELD_CLOSED_AT to null)

    pollRef.updateChildren(updates).await()
  }

  override suspend fun deletePoll(conversationId: String, pollId: String, userId: String) {
    val pollRef = conversationsRef.child(conversationId).child(POLLS_PATH).child(pollId)

    // Verify ownership
    val snapshot = pollRef.get().await()
    val poll =
        dataSnapshotToPoll(snapshot, conversationId) ?: throw Exception("Poll not found: $pollId")

    if (poll.creatorId != userId) {
      throw Exception("Only the poll owner can delete this poll")
    }

    pollRef.removeValue().await()
  }

  /**
   * Converts a [Poll] object to a map for storing in Realtime Database.
   *
   * @param poll The poll to convert
   * @return Map of field names to values
   */
  private fun pollToMap(poll: Poll): Map<String, Any?> {
    return mapOf(
        FIELD_CREATOR_ID to poll.creatorId,
        FIELD_CREATOR_NAME to poll.creatorName,
        FIELD_QUESTION to poll.question,
        FIELD_OPTIONS to optionsToList(poll.options),
        FIELD_IS_ANONYMOUS to poll.isAnonymous,
        FIELD_ALLOW_MULTIPLE_ANSWERS to poll.allowMultipleAnswers,
        FIELD_IS_CLOSED to poll.isClosed,
        FIELD_CREATED_AT to poll.createdAt,
        FIELD_CLOSED_AT to poll.closedAt)
  }

  /**
   * Converts a list of [PollOption] objects to a list of maps for storing in Realtime Database.
   *
   * @param options The options to convert
   * @return List of maps representing options
   */
  private fun optionsToList(options: List<PollOption>): List<Map<String, Any?>> {
    return options.map { option ->
      mapOf(
          FIELD_OPTION_ID to option.id,
          FIELD_OPTION_TEXT to option.text,
          FIELD_OPTION_VOTER_IDS to option.voterIds)
    }
  }

  /**
   * Converts a Realtime Database DataSnapshot to a [Poll] object.
   *
   * @param snapshot The DataSnapshot from Realtime Database
   * @param conversationId The conversation ID this poll belongs to
   * @return The [Poll] object, or null if conversion fails
   */
  private fun dataSnapshotToPoll(snapshot: DataSnapshot, conversationId: String): Poll? {
    return try {
      val id = snapshot.key ?: return null
      val creatorId = snapshot.child(FIELD_CREATOR_ID).getValue(String::class.java) ?: return null
      val creatorName =
          snapshot.child(FIELD_CREATOR_NAME).getValue(String::class.java) ?: return null
      val question = snapshot.child(FIELD_QUESTION).getValue(String::class.java) ?: return null
      val createdAt = snapshot.child(FIELD_CREATED_AT).getValue(Long::class.java) ?: return null

      val isAnonymous = snapshot.child(FIELD_IS_ANONYMOUS).getValue(Boolean::class.java) ?: false
      val allowMultipleAnswers =
          snapshot.child(FIELD_ALLOW_MULTIPLE_ANSWERS).getValue(Boolean::class.java) ?: false
      val isClosed = snapshot.child(FIELD_IS_CLOSED).getValue(Boolean::class.java) ?: false
      val closedAt = snapshot.child(FIELD_CLOSED_AT).getValue(Long::class.java)

      // Parse options
      val options = mutableListOf<PollOption>()
      for (optionSnapshot in snapshot.child(FIELD_OPTIONS).children) {
        val optionId =
            optionSnapshot.child(FIELD_OPTION_ID).getValue(Int::class.java)
                ?: optionSnapshot.key?.toIntOrNull()
                ?: continue
        val optionText =
            optionSnapshot.child(FIELD_OPTION_TEXT).getValue(String::class.java) ?: continue
        val voterIds =
            optionSnapshot.child(FIELD_OPTION_VOTER_IDS).getValue(STRING_LIST_TYPE_INDICATOR)
                ?: emptyList()

        options.add(PollOption(id = optionId, text = optionText, voterIds = voterIds))
      }

      Poll(
          id = id,
          conversationId = conversationId,
          creatorId = creatorId,
          creatorName = creatorName,
          question = question,
          options = options.sortedBy { it.id },
          isAnonymous = isAnonymous,
          allowMultipleAnswers = allowMultipleAnswers,
          isClosed = isClosed,
          createdAt = createdAt,
          closedAt = closedAt)
    } catch (e: Exception) {
      Log.e(TAG, "Error converting snapshot to Poll", e)
      null
    }
  }
}
