package com.android.joinme.model.presence

// Implemented with help of Claude AI

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive tests for PresenceRepositoryLocal.
 *
 * Tests all operations: setUserOnline, setUserOffline, observeOnlineUsersCount,
 * observeOnlineUserIds, and multi-context isolation.
 */
class PresenceRepositoryLocalTest {

  private lateinit var repo: PresenceRepositoryLocal
  private val testContextId = "context1"
  private val testUserId = "user1"
  private val currentUserId = "currentUser"

  @Before
  fun setup() {
    repo = PresenceRepositoryLocal()
  }

  // ---------------- SET USER ONLINE ----------------

  @Test
  fun setUserOnline_singleContext_setsUserAsOnlineWithCorrectData() = runTest {
    val beforeTime = System.currentTimeMillis()
    repo.setUserOnline(testUserId, listOf(testContextId))
    val afterTime = System.currentTimeMillis()

    val presenceData = repo.getPresenceDataForContext(testContextId)
    assertNotNull(presenceData)

    val entry = presenceData!![testUserId]!!
    assertTrue(entry.online)
    assertEquals(testUserId, entry.visitorId)
    assertTrue(entry.lastSeen in beforeTime..afterTime)
  }

  @Test
  fun setUserOnline_multipleContexts_setsUserOnlineInAllAndUpdatesIndex() = runTest {
    val contexts = listOf("context1", "context2", "context3")
    repo.setUserOnline(testUserId, contexts)

    // Verify presence in all contexts
    for (contextId in contexts) {
      val presenceData = repo.getPresenceDataForContext(contextId)
      assertNotNull(presenceData)
      assertTrue(presenceData!![testUserId]?.online == true)
    }

    // Verify user contexts index
    val userContexts = repo.getUserContexts(testUserId)
    assertNotNull(userContexts)
    assertEquals(3, userContexts!!.size)
    assertTrue(userContexts.containsAll(contexts))
  }

  @Test
  fun setUserOnline_blankUserIdOrContextId_handlesGracefully() = runTest {
    // Blank userId should do nothing
    repo.setUserOnline("", listOf(testContextId))
    assertNull(repo.getPresenceDataForContext(testContextId))

    // Blank contextId should be skipped
    repo.setUserOnline(testUserId, listOf("", testContextId))
    assertNull(repo.getPresenceDataForContext(""))
    assertNotNull(repo.getPresenceDataForContext(testContextId))
  }

  // ---------------- SET USER OFFLINE ----------------

  @Test
  fun setUserOffline_setsUserAsOfflineInAllContexts() = runTest {
    val contexts = listOf("context1", "context2", "context3")
    repo.setUserOnline(testUserId, contexts)
    repo.setUserOffline(testUserId)

    for (contextId in contexts) {
      val presenceData = repo.getPresenceDataForContext(contextId)
      assertFalse(presenceData!![testUserId]?.online!!)
    }
  }

  @Test
  fun setUserOffline_updatesLastSeenTimestamp() = runTest {
    repo.setUserOnline(testUserId, listOf(testContextId))

    val beforeOffline = System.currentTimeMillis()
    repo.setUserOffline(testUserId)
    val afterOffline = System.currentTimeMillis()

    val presenceData = repo.getPresenceDataForContext(testContextId)
    val lastSeen = presenceData!![testUserId]?.lastSeen!!
    assertTrue(lastSeen in beforeOffline..afterOffline)
  }

  @Test
  fun setUserOffline_blankOrNonExistentUser_doesNothing() = runTest {
    repo.setUserOnline(testUserId, listOf(testContextId))

    // Blank userId
    repo.setUserOffline("")
    assertTrue(repo.getPresenceDataForContext(testContextId)!![testUserId]?.online!!)

    // Non-existent user
    repo.setUserOffline("nonExistentUser")
    assertTrue(repo.getPresenceDataForContext(testContextId)!![testUserId]?.online!!)
  }

  // ---------------- OBSERVE ONLINE USERS COUNT ----------------

  @Test
  fun observeOnlineUsersCount_countsOnlineUsersExcludingCurrentUser() = runTest {
    // Initially zero
    assertEquals(0, repo.observeOnlineUsersCount(testContextId, currentUserId).first())

    // Add users
    repo.setUserOnline(currentUserId, listOf(testContextId))
    repo.setUserOnline("user1", listOf(testContextId))
    repo.setUserOnline("user2", listOf(testContextId))

    // Should exclude current user
    assertEquals(2, repo.observeOnlineUsersCount(testContextId, currentUserId).first())

    // Set one offline
    repo.setUserOffline("user1")
    assertEquals(1, repo.observeOnlineUsersCount(testContextId, currentUserId).first())
  }

  @Test
  fun observeOnlineUsersCount_blankInputs_returnsZero() = runTest {
    repo.setUserOnline(testUserId, listOf(testContextId))

    assertEquals(0, repo.observeOnlineUsersCount("", currentUserId).first())
    assertEquals(0, repo.observeOnlineUsersCount(testContextId, "").first())
    assertEquals(0, repo.observeOnlineUsersCount("nonExistent", currentUserId).first())
  }

  // ---------------- OBSERVE ONLINE USER IDS ----------------

  @Test
  fun observeOnlineUserIds_returnsOnlineUserIdsExcludingCurrentUser() = runTest {
    // Initially empty
    assertTrue(repo.observeOnlineUserIds(testContextId, currentUserId).first().isEmpty())

    // Add users
    repo.setUserOnline(currentUserId, listOf(testContextId))
    repo.setUserOnline("user1", listOf(testContextId))
    repo.setUserOnline("user2", listOf(testContextId))

    val userIds = repo.observeOnlineUserIds(testContextId, currentUserId).first()
    assertEquals(2, userIds.size)
    assertFalse(userIds.contains(currentUserId))
    assertTrue(userIds.containsAll(listOf("user1", "user2")))

    // Set one offline
    repo.setUserOffline("user1")
    val updatedIds = repo.observeOnlineUserIds(testContextId, currentUserId).first()
    assertEquals(1, updatedIds.size)
    assertFalse(updatedIds.contains("user1"))
    assertTrue(updatedIds.contains("user2"))
  }

  @Test
  fun observeOnlineUserIds_blankInputs_returnsEmptyList() = runTest {
    repo.setUserOnline(testUserId, listOf(testContextId))

    assertTrue(repo.observeOnlineUserIds("", currentUserId).first().isEmpty())
    assertTrue(repo.observeOnlineUserIds(testContextId, "").first().isEmpty())
  }

  // ---------------- MULTI-CONTEXT ISOLATION ----------------

  @Test
  fun multiContext_presenceIsIsolatedPerContext() = runTest {
    repo.setUserOnline("user1", listOf("context1"))
    repo.setUserOnline("user2", listOf("context2"))
    repo.setUserOnline("user3", listOf("context1", "context2"))

    assertEquals(2, repo.observeOnlineUsersCount("context1", currentUserId).first())
    assertEquals(2, repo.observeOnlineUsersCount("context2", currentUserId).first())

    // Setting user3 offline affects both contexts
    repo.setUserOffline("user3")
    assertEquals(1, repo.observeOnlineUsersCount("context1", currentUserId).first())
    assertEquals(1, repo.observeOnlineUsersCount("context2", currentUserId).first())
  }

  // ---------------- CLEAR ALL ----------------

  @Test
  fun clearAll_removesAllDataAndUpdatesFlow() = runTest {
    repo.setUserOnline("user1", listOf("context1", "context2"))
    repo.setUserOnline("user2", listOf("context2"))
    repo.clearAll()

    assertNull(repo.getPresenceDataForContext("context1"))
    assertNull(repo.getPresenceDataForContext("context2"))
    assertNull(repo.getUserContexts("user1"))
    assertEquals(0, repo.observeOnlineUsersCount("context1", currentUserId).first())
  }
}
