package com.android.joinme.model.database

// Tests partially written with AI assistance; reviewed for correctness.

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GroupDaoTest {

  private lateinit var database: AppDatabase
  private lateinit var groupDao: GroupDao

  private val testGroup1 =
      GroupEntity(
          id = "test-group-1",
          name = "Test Group 1",
          category = "SPORTS",
          description = "A test sports group",
          ownerId = "owner1",
          memberIdsJson = "[\"member1\",\"member2\",\"member3\"]",
          eventIdsJson = "[\"event1\",\"event2\"]",
          serieIdsJson = "[\"serie1\"]",
          photoUrl = "https://example.com/photo1.jpg",
          cachedAt = System.currentTimeMillis())

  private val testGroup2 =
      GroupEntity(
          id = "test-group-2",
          name = "Test Group 2",
          category = "ACTIVITY",
          description = "A test activity group",
          ownerId = "owner2",
          memberIdsJson = "[\"member4\",\"member5\"]",
          eventIdsJson = "[]",
          serieIdsJson = "[\"serie2\",\"serie3\"]",
          photoUrl = "https://example.com/photo2.jpg",
          cachedAt = System.currentTimeMillis())

  private val testGroup3 =
      GroupEntity(
          id = "test-group-3",
          name = "Test Group 3",
          category = "LEARNING",
          description = "A test learning group",
          ownerId = "owner1",
          memberIdsJson = "[]",
          eventIdsJson = "[]",
          serieIdsJson = "[]",
          photoUrl = null,
          cachedAt = System.currentTimeMillis() - 10000) // Older cache

  @Before
  fun setup() {
    // Create an in-memory database for testing
    database =
        Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    groupDao = database.groupDao()
  }

  @After
  fun tearDown() {
    database.close()
  }

  // ==================== Insert Tests ====================

  @Test
  fun insertGroup_insertsSuccessfully() = runTest {
    // When
    groupDao.insertGroup(testGroup1)

    // Then
    val retrieved = groupDao.getGroupById(testGroup1.id)
    assertNotNull(retrieved)
    assertEquals(testGroup1.id, retrieved?.id)
    assertEquals(testGroup1.name, retrieved?.name)
    assertEquals(testGroup1.category, retrieved?.category)
  }

  @Test
  fun insertGroup_withNullFields_insertsSuccessfully() = runTest {
    // When
    groupDao.insertGroup(testGroup3)

    // Then
    val retrieved = groupDao.getGroupById(testGroup3.id)
    assertNotNull(retrieved)
    assertEquals(testGroup3.id, retrieved?.id)
    assertNull(retrieved?.photoUrl)
  }

  @Test
  fun insertGroup_replacesExistingGroup() = runTest {
    // Given
    groupDao.insertGroup(testGroup1)

    // When - Insert group with same ID but different data
    val updatedGroup = testGroup1.copy(name = "Updated Name", description = "Updated description")
    groupDao.insertGroup(updatedGroup)

    // Then
    val retrieved = groupDao.getGroupById(testGroup1.id)
    assertNotNull(retrieved)
    assertEquals("Updated Name", retrieved?.name)
    assertEquals("Updated description", retrieved?.description)

    // Verify only one group exists
    val allGroups = groupDao.getAllGroups()
    assertEquals(1, allGroups.size)
  }

  @Test
  fun insertGroups_insertsMultipleGroupsSuccessfully() = runTest {
    // When
    groupDao.insertGroups(listOf(testGroup1, testGroup2, testGroup3))

    // Then
    val allGroups = groupDao.getAllGroups()
    assertEquals(3, allGroups.size)

    val ids = allGroups.map { it.id }
    assertTrue(ids.contains(testGroup1.id))
    assertTrue(ids.contains(testGroup2.id))
    assertTrue(ids.contains(testGroup3.id))
  }

  @Test
  fun insertGroups_withEmptyList_doesNothing() = runTest {
    // When
    groupDao.insertGroups(emptyList())

    // Then
    val allGroups = groupDao.getAllGroups()
    assertTrue(allGroups.isEmpty())
  }

  @Test
  fun insertGroups_replacesDuplicates() = runTest {
    // Given
    groupDao.insertGroups(listOf(testGroup1, testGroup2))

    // When - Insert list with one updated group and one new group
    val updatedGroup1 = testGroup1.copy(name = "UpdatedGroup1")
    groupDao.insertGroups(listOf(updatedGroup1, testGroup3))

    // Then
    val allGroups = groupDao.getAllGroups()
    assertEquals(3, allGroups.size) // 2 original + 1 new

    val group1 = groupDao.getGroupById(testGroup1.id)
    assertEquals("UpdatedGroup1", group1?.name)
  }

  // ==================== Get Tests ====================

  @Test
  fun getGroupById_returnsNullForNonexistentGroup() = runTest {
    // When
    val result = groupDao.getGroupById("nonexistent-id")

    // Then
    assertNull(result)
  }

  @Test
  fun getGroupById_returnsCorrectGroup() = runTest {
    // Given
    groupDao.insertGroups(listOf(testGroup1, testGroup2, testGroup3))

    // When
    val result = groupDao.getGroupById(testGroup2.id)

    // Then
    assertNotNull(result)
    assertEquals(testGroup2.id, result?.id)
    assertEquals(testGroup2.name, result?.name)
    assertEquals(testGroup2.category, result?.category)
  }

  @Test
  fun getAllGroups_returnsEmptyListWhenDatabaseEmpty() = runTest {
    // When
    val result = groupDao.getAllGroups()

    // Then
    assertTrue(result.isEmpty())
  }

  @Test
  fun getAllGroups_returnsAllGroups() = runTest {
    // Given
    groupDao.insertGroups(listOf(testGroup1, testGroup2, testGroup3))

    // When
    val result = groupDao.getAllGroups()

    // Then
    assertEquals(3, result.size)
    val ids = result.map { it.id }
    assertTrue(ids.contains(testGroup1.id))
    assertTrue(ids.contains(testGroup2.id))
    assertTrue(ids.contains(testGroup3.id))
  }

  // ==================== Delete Tests ====================

  @Test
  fun deleteGroup_removesSpecificGroup() = runTest {
    // Given
    groupDao.insertGroups(listOf(testGroup1, testGroup2, testGroup3))

    // When
    groupDao.deleteGroup(testGroup2.id)

    // Then
    val result = groupDao.getGroupById(testGroup2.id)
    assertNull(result)

    // Verify other groups still exist
    val allGroups = groupDao.getAllGroups()
    assertEquals(2, allGroups.size)
    assertNotNull(groupDao.getGroupById(testGroup1.id))
    assertNotNull(groupDao.getGroupById(testGroup3.id))
  }

  @Test
  fun deleteGroup_withNonexistentId_doesNothing() = runTest {
    // Given
    groupDao.insertGroup(testGroup1)

    // When
    groupDao.deleteGroup("nonexistent-id")

    // Then - Original group still exists
    val allGroups = groupDao.getAllGroups()
    assertEquals(1, allGroups.size)
  }

  @Test
  fun deleteAllGroups_removesAllGroups() = runTest {
    // Given
    groupDao.insertGroups(listOf(testGroup1, testGroup2, testGroup3))

    // When
    groupDao.deleteAllGroups()

    // Then
    val result = groupDao.getAllGroups()
    assertTrue(result.isEmpty())
  }

  @Test
  fun deleteAllGroups_onEmptyDatabase_doesNothing() = runTest {
    // When
    groupDao.deleteAllGroups()

    // Then
    val result = groupDao.getAllGroups()
    assertTrue(result.isEmpty())
  }

  @Test
  fun deleteOldGroups_removesGroupsBeforeTimestamp() = runTest {
    // Given
    val currentTime = System.currentTimeMillis()
    val oldGroup1 = testGroup1.copy(id = "old-1", cachedAt = currentTime - 10000) // 10 seconds ago
    val oldGroup2 = testGroup2.copy(id = "old-2", cachedAt = currentTime - 5000) // 5 seconds ago
    val recentGroup = testGroup3.copy(id = "recent", cachedAt = currentTime - 1000) // 1 second ago

    groupDao.insertGroups(listOf(oldGroup1, oldGroup2, recentGroup))

    // When - Delete groups older than 3 seconds ago
    val cutoffTime = currentTime - 3000
    groupDao.deleteOldGroups(cutoffTime)

    // Then
    val remainingGroups = groupDao.getAllGroups()
    assertEquals(1, remainingGroups.size)
    assertEquals("recent", remainingGroups[0].id)

    // Verify old groups are deleted
    assertNull(groupDao.getGroupById("old-1"))
    assertNull(groupDao.getGroupById("old-2"))
  }

  @Test
  fun deleteOldGroups_withFutureTimestamp_removesAllGroups() = runTest {
    // Given
    groupDao.insertGroups(listOf(testGroup1, testGroup2, testGroup3))

    // When - Delete groups older than a future timestamp
    val futureTime = System.currentTimeMillis() + 10000
    groupDao.deleteOldGroups(futureTime)

    // Then
    val remainingGroups = groupDao.getAllGroups()
    assertTrue(remainingGroups.isEmpty())
  }

  @Test
  fun deleteOldGroups_withVeryOldTimestamp_removesNoGroups() = runTest {
    // Given
    groupDao.insertGroups(listOf(testGroup1, testGroup2, testGroup3))

    // When - Delete groups older than a very old timestamp
    val veryOldTime = 1000L // Very old timestamp
    groupDao.deleteOldGroups(veryOldTime)

    // Then
    val remainingGroups = groupDao.getAllGroups()
    assertEquals(3, remainingGroups.size)
  }

  // ==================== Complex Scenario Tests ====================

  @Test
  fun complexScenario_insertUpdateDeleteOperations() = runTest {
    // Insert initial groups
    groupDao.insertGroups(listOf(testGroup1, testGroup2))
    assertEquals(2, groupDao.getAllGroups().size)

    // Update one group
    val updatedGroup1 = testGroup1.copy(name = "Updated Name", ownerId = "newOwner")
    groupDao.insertGroup(updatedGroup1)
    val retrieved1 = groupDao.getGroupById(testGroup1.id)
    assertEquals("Updated Name", retrieved1?.name)
    assertEquals("newOwner", retrieved1?.ownerId)

    // Add a new group
    groupDao.insertGroup(testGroup3)
    assertEquals(3, groupDao.getAllGroups().size)

    // Delete one group
    groupDao.deleteGroup(testGroup2.id)
    assertEquals(2, groupDao.getAllGroups().size)
    assertNull(groupDao.getGroupById(testGroup2.id))

    // Verify remaining groups
    assertNotNull(groupDao.getGroupById(testGroup1.id))
    assertNotNull(groupDao.getGroupById(testGroup3.id))
  }

  @Test
  fun testDataIntegrity_allFieldsPreserved() = runTest {
    // Given
    val group =
        GroupEntity(
            id = "integrity-test-id",
            name = "Integrity Test Group",
            category = "NIGHTLIFE",
            description = "Testing data integrity with special chars: !@#$%^&*()",
            ownerId = "owner-integrity",
            memberIdsJson = "[\"member1\",\"member2\",\"member3\",\"member4\"]",
            eventIdsJson = "[\"event1\",\"event2\",\"event3\"]",
            serieIdsJson = "[\"serie1\",\"serie2\"]",
            photoUrl = "https://example.com/integrity-photo.jpg",
            cachedAt = System.currentTimeMillis())

    // When
    groupDao.insertGroup(group)
    val retrieved = groupDao.getGroupById(group.id)

    // Then - Verify all fields are preserved exactly
    assertNotNull(retrieved)
    assertEquals(group.id, retrieved?.id)
    assertEquals(group.name, retrieved?.name)
    assertEquals(group.category, retrieved?.category)
    assertEquals(group.description, retrieved?.description)
    assertEquals(group.ownerId, retrieved?.ownerId)
    assertEquals(group.memberIdsJson, retrieved?.memberIdsJson)
    assertEquals(group.eventIdsJson, retrieved?.eventIdsJson)
    assertEquals(group.serieIdsJson, retrieved?.serieIdsJson)
    assertEquals(group.photoUrl, retrieved?.photoUrl)
    assertEquals(group.cachedAt, retrieved?.cachedAt)
  }

  @Test
  fun testConcurrentOperations_maintainsConsistency() = runTest {
    // Insert multiple groups
    groupDao.insertGroups(listOf(testGroup1, testGroup2, testGroup3))

    // Perform multiple operations
    groupDao.deleteGroup(testGroup1.id)
    groupDao.insertGroup(testGroup1.copy(name = "Reinserted"))
    val updated = testGroup2.copy(description = "Updated concurrently")
    groupDao.insertGroup(updated)

    // Verify final state
    val allGroups = groupDao.getAllGroups()
    assertEquals(3, allGroups.size)

    val group1 = groupDao.getGroupById(testGroup1.id)
    assertEquals("Reinserted", group1?.name)

    val group2 = groupDao.getGroupById(testGroup2.id)
    assertEquals("Updated concurrently", group2?.description)
  }
}
