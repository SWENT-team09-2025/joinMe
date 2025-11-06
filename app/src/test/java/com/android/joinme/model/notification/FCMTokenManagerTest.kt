package com.android.joinme.model.notification

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class FCMTokenManagerTest {

  private lateinit var mockContext: Context
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var mockDb: FirebaseFirestore
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocument: DocumentReference
  private lateinit var mockMessaging: FirebaseMessaging

  private val testUserId = "testUser123"
  private val testToken = "test_fcm_token_abc123"

  @Before
  fun setup() {
    // Mock Context
    mockContext = mockk(relaxed = true)

    // Mock Firebase Auth
    mockAuth = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)
    every { mockUser.uid } returns testUserId

    // Mock Firestore
    mockDb = mockk(relaxed = true)
    mockCollection = mockk(relaxed = true)
    mockDocument = mockk(relaxed = true)
    every { mockDb.collection("profiles") } returns mockCollection
    every { mockCollection.document(testUserId) } returns mockDocument

    // Mock Firebase Messaging
    mockMessaging = mockk(relaxed = true)

    // Mock static methods
    mockkStatic(FirebaseAuth::class)
    mockkStatic(FirebaseFirestore::class)
    mockkStatic(FirebaseMessaging::class)

    every { FirebaseAuth.getInstance() } returns mockAuth
    every { FirebaseFirestore.getInstance() } returns mockDb
    every { FirebaseMessaging.getInstance() } returns mockMessaging
  }

  @After
  fun tearDown() {
    unmockkStatic(FirebaseAuth::class)
    unmockkStatic(FirebaseFirestore::class)
    unmockkStatic(FirebaseMessaging::class)
  }

  @Test
  fun `initializeFCMToken with logged in user retrieves and saves token`() = runTest {
    // Given
    every { mockAuth.currentUser } returns mockUser
    every { mockMessaging.token } returns Tasks.forResult(testToken)
    every { mockDocument.set(mapOf("fcmToken" to testToken), SetOptions.merge()) } returns Tasks.forResult(null)

    // When
    FCMTokenManager.initializeFCMToken(mockContext)

    // Wait for coroutine to complete
    Thread.sleep(100)

    // Then
    verify { mockMessaging.token }
    verify { mockDocument.set(mapOf("fcmToken" to testToken), SetOptions.merge()) }
  }

  @Test
  fun `initializeFCMToken with no user logged in does nothing`() = runTest {
    // Given
    every { mockAuth.currentUser } returns null

    // When
    FCMTokenManager.initializeFCMToken(mockContext)

    // Wait to ensure no operations happen
    Thread.sleep(100)

    // Then
    verify(exactly = 0) { mockMessaging.token }
    verify(exactly = 0) { mockDocument.set(any<Map<String, Any?>>(), any<SetOptions>()) }
  }

  @Test
  fun `updateFCMToken with logged in user updates token in Firestore`() = runTest {
    // Given
    every { mockAuth.currentUser } returns mockUser
    every { mockDocument.set(mapOf("fcmToken" to testToken), SetOptions.merge()) } returns Tasks.forResult(null)

    // When
    FCMTokenManager.updateFCMToken(testToken)

    // Wait for coroutine to complete
    Thread.sleep(100)

    // Then
    verify { mockCollection.document(testUserId) }
    verify { mockDocument.set(mapOf("fcmToken" to testToken), SetOptions.merge()) }
  }

  @Test
  fun `updateFCMToken with no user logged in does nothing`() = runTest {
    // Given
    every { mockAuth.currentUser } returns null

    // When
    FCMTokenManager.updateFCMToken(testToken)

    // Wait to ensure no operations happen
    Thread.sleep(100)

    // Then
    verify(exactly = 0) { mockDocument.set(any<Map<String, Any?>>(), any<SetOptions>()) }
  }

  @Test
  fun `clearFCMToken with logged in user clears token in Firestore`() = runTest {
    // Given
    every { mockAuth.currentUser } returns mockUser
    every { mockDocument.set(mapOf("fcmToken" to null), SetOptions.merge()) } returns Tasks.forResult(null)

    // When
    FCMTokenManager.clearFCMToken()

    // Wait for coroutine to complete
    Thread.sleep(100)

    // Then
    verify { mockCollection.document(testUserId) }
    verify { mockDocument.set(mapOf("fcmToken" to null), SetOptions.merge()) }
  }

  @Test
  fun `clearFCMToken with no user logged in does nothing`() = runTest {
    // Given
    every { mockAuth.currentUser } returns null

    // When
    FCMTokenManager.clearFCMToken()

    // Wait to ensure no operations happen
    Thread.sleep(100)

    // Then
    verify(exactly = 0) { mockDocument.set(any<Map<String, Any?>>(), any<SetOptions>()) }
  }

  @Test
  fun `initializeFCMToken handles exception gracefully`() = runTest {
    // Given
    every { mockAuth.currentUser } returns mockUser
    every { mockMessaging.token } returns Tasks.forException(RuntimeException("Token fetch failed"))

    // When
    FCMTokenManager.initializeFCMToken(mockContext)

    // Wait for coroutine to complete
    Thread.sleep(100)

    // Then - should not crash, just log error
    verify { mockMessaging.token }
    verify(exactly = 0) { mockDocument.set(any<Map<String, Any?>>(), any<SetOptions>()) }
  }

  @Test
  fun `updateFCMToken handles exception gracefully`() = runTest {
    // Given
    every { mockAuth.currentUser } returns mockUser
    every { mockDocument.set(mapOf("fcmToken" to testToken), SetOptions.merge()) } returns
        Tasks.forException(RuntimeException("Firestore update failed"))

    // When
    FCMTokenManager.updateFCMToken(testToken)

    // Wait for coroutine to complete
    Thread.sleep(100)

    // Then - should not crash, just log error
    verify { mockDocument.set(mapOf("fcmToken" to testToken), SetOptions.merge()) }
  }

  @Test
  fun `clearFCMToken handles exception gracefully`() = runTest {
    // Given
    every { mockAuth.currentUser } returns mockUser
    every { mockDocument.set(mapOf("fcmToken" to null), SetOptions.merge()) } returns
        Tasks.forException(RuntimeException("Firestore update failed"))

    // When
    FCMTokenManager.clearFCMToken()

    // Wait for coroutine to complete
    Thread.sleep(100)

    // Then - should not crash, just log error
    verify { mockDocument.set(mapOf("fcmToken" to null), SetOptions.merge()) }
  }
}
