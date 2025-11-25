package com.android.joinme.model.groups.streaks

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Provides a singleton instance of the GroupStreakRepository for dependency injection.
 *
 * This provider follows the repository pattern and enables easy testing by allowing the repository
 * instance to be swapped with a mock or fake implementation. Returns a local repository in test
 * environments and a Firestore-backed implementation in production.
 */
object GroupStreakRepositoryProvider {

    private var firestoreRepo: GroupStreakRepository? = null
    private val localRepo: GroupStreakRepository by lazy { GroupStreakRepositoryLocal() }

    /**
     * Returns the appropriate repository based on the environment.
     *
     * @param context Optional Android context for Firebase initialization.
     * @return A [GroupStreakRepository] instance.
     */
    fun getRepository(context: Context? = null): GroupStreakRepository {
        return if (isTestEnvironment()) {
            localRepo
        } else {
            getFirestoreRepo(context)
        }
    }

    /**
     * Returns the Firestore-backed repository, initializing Firebase if needed.
     *
     * @param context Optional Android context for Firebase initialization.
     * @return A [GroupStreakRepositoryFirestore] instance.
     */
    private fun getFirestoreRepo(context: Context?): GroupStreakRepository {
        if (firestoreRepo == null) {
            context?.let {
                if (FirebaseApp.getApps(it).isEmpty()) {
                    FirebaseApp.initializeApp(it)
                }
            }
            firestoreRepo = GroupStreakRepositoryFirestore(FirebaseFirestore.getInstance())
        }
        return firestoreRepo!!
    }

    /**
     * Detects if the code is running in a test environment.
     *
     * @return True if running in a test environment, false otherwise.
     */
    private fun isTestEnvironment(): Boolean {
        return try {
            Class.forName("org.junit.Test")
            true
        } catch (_: ClassNotFoundException) {
            System.getProperty("IS_TEST_ENV") == "true"
        }
    }
}