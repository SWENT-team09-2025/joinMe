package com.android.joinme.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

object GroupRepositoryProvider {
    private val _repository: GroupRepository by lazy { GroupRepositoryFirestore(Firebase.firestore) }
    var repository: GroupRepository = _repository
}