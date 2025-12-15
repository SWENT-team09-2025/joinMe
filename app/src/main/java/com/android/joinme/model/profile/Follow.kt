package com.android.joinme.model.profile

/**
 * Represents a follow relationship between two users.
 *
 * This is a lightweight model stored in a separate Firestore collection for scalability. By using a
 * separate collection instead of embedded arrays in Profile documents, we can support unlimited
 * followers and enable efficient queries.
 *
 * @property id Unique identifier for this follow relationship (Firestore auto-generated document
 *   ID)
 * @property followerId The user ID of the person doing the following
 * @property followedId The user ID of the person being followed
 */
data class Follow(val id: String = "", val followerId: String = "", val followedId: String = "")
