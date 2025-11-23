package com.android.joinme.model.profile

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Profile(
    @DocumentId val uid: String = "",
    val photoUrl: String? = null,
    val username: String = "",
    val email: String = "",
    val dateOfBirth: String? = null,
    val country: String? = null,
    val interests: List<String> = emptyList(),
    val bio: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val fcmToken: String? = null,
    val eventsJoinedCount: Int = 0
)
