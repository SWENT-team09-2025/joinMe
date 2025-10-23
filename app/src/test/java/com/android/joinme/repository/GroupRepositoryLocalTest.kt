package com.android.joinme.repository

import com.android.joinme.model.event.EventType
import com.android.joinme.model.group.Group
import com.android.joinme.model.group.GroupRepositoryLocal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GroupRepositoryLocal.
 *
 * Tests the local in-memory repository implementation without Firebase dependencies.
 */
class GroupRepositoryLocalTest {

  private lateinit var repository: GroupRepositoryLocal

  @Before
  fun setUp() {
    repository = GroupRepositoryLocal()
  }

  @Test
  fun userGroups_initiallyReturnsEmptyList() = runTest {
    val groups = repository.userGroups()

    assertTrue(groups.isEmpty())
  }

  @Test
  fun getGroup_withNonExistentId_returnsNull() = runTest {
    val result = repository.getGroup("non-existent-id")
    assertNull(result)
  }

  /**
   * Tests using reflection to add test data to the internal groups list. This allows us to test the
   * full behavior of the repository.
   */
  @Test
  fun leaveGroup_afterAddingGroups_removesCorrectGroup() = runTest {
    val testGroups =
        listOf(
            Group(
                id = "1", name = "Group 1", ownerId = "owner1", memberIds = List(10) { "user$it" }),
            Group(
                id = "2", name = "Group 2", ownerId = "owner2", memberIds = List(20) { "user$it" }),
            Group(
                id = "3", name = "Group 3", ownerId = "owner3", memberIds = List(30) { "user$it" }))

    addGroupsViaReflection(testGroups)

    repository.leaveGroup("2")

    val remainingGroups = repository.userGroups()
    assertEquals(2, remainingGroups.size)
    assertFalse(remainingGroups.any { it.id == "2" })
    assertTrue(remainingGroups.any { it.id == "1" })
    assertTrue(remainingGroups.any { it.id == "3" })
  }

  @Test
  fun leaveGroup_withFirstGroup_removesCorrectly() = runTest {
    val testGroups =
        listOf(
            Group(id = "1", name = "First", ownerId = "owner1"),
            Group(id = "2", name = "Second", ownerId = "owner2"),
            Group(id = "3", name = "Third", ownerId = "owner3"))
    addGroupsViaReflection(testGroups)

    repository.leaveGroup("1")

    val remainingGroups = repository.userGroups()
    assertEquals(2, remainingGroups.size)
    assertFalse(remainingGroups.any { it.id == "1" })
  }

  @Test
  fun leaveGroup_withLastGroup_removesCorrectly() = runTest {
    val testGroups =
        listOf(
            Group(id = "1", name = "First", ownerId = "owner1"),
            Group(id = "2", name = "Second", ownerId = "owner2"),
            Group(id = "3", name = "Third", ownerId = "owner3"))
    addGroupsViaReflection(testGroups)

    repository.leaveGroup("3")
    val remainingGroups = repository.userGroups()
    assertEquals(2, remainingGroups.size)
    assertFalse(remainingGroups.any { it.id == "3" })
  }

  @Test
  fun leaveGroup_withOnlyGroup_leavesEmptyList() = runTest {
    val testGroups = listOf(Group(id = "1", name = "Only Group", ownerId = "owner1"))
    addGroupsViaReflection(testGroups)

    repository.leaveGroup("1")

    val remainingGroups = repository.userGroups()
    assertTrue(remainingGroups.isEmpty())
  }

  @Test
  fun leaveGroup_multipleTimes_removesAllCorrectly() = runTest {
    val testGroups =
        listOf(
            Group(id = "1", name = "Group 1", ownerId = "owner1"),
            Group(id = "2", name = "Group 2", ownerId = "owner2"),
            Group(id = "3", name = "Group 3", ownerId = "owner3"),
            Group(id = "4", name = "Group 4", ownerId = "owner4"))
    addGroupsViaReflection(testGroups)

    repository.leaveGroup("2")
    repository.leaveGroup("4")

    val remainingGroups = repository.userGroups()
    assertEquals(2, remainingGroups.size)
    assertTrue(remainingGroups.any { it.id == "1" })
    assertTrue(remainingGroups.any { it.id == "3" })
  }

  @Test
  fun getGroup_withValidId_returnsCorrectGroup() = runTest {
    val testGroups =
        listOf(
            Group(
                id = "id-0",
                name = "First Group",
                ownerId = "owner1",
                memberIds = List(10) { "user$it" }),
            Group(
                id = "id-1",
                name = "Second Group",
                ownerId = "owner2",
                memberIds = List(20) { "user$it" }),
            Group(
                id = "id-2",
                name = "Third Group",
                ownerId = "owner3",
                memberIds = List(30) { "user$it" }))
    addGroupsViaReflection(testGroups)

    val group0 = repository.getGroup("id-0")
    val group1 = repository.getGroup("id-1")
    val group2 = repository.getGroup("id-2")

    assertNotNull(group0)
    assertEquals("First Group", group0?.name)
    assertEquals(10, group0?.membersCount)

    assertNotNull(group1)
    assertEquals("Second Group", group1?.name)
    assertEquals(20, group1?.membersCount)

    assertNotNull(group2)
    assertEquals("Third Group", group2?.name)
    assertEquals(30, group2?.membersCount)
  }

  @Test
  fun getGroup_withNonMatchingId_returnsNull() = runTest {
    val testGroups = listOf(Group(id = "1", name = "Only Group", ownerId = "owner1"))
    addGroupsViaReflection(testGroups)

    val result = repository.getGroup("999")
    assertNull(result)
  }

  @Test
  fun getGroup_afterLeaving_returnsNull() = runTest {
    val testGroups = listOf(Group(id = "1", name = "Test", ownerId = "owner1"))
    addGroupsViaReflection(testGroups)

    repository.leaveGroup("1")
    val result = repository.getGroup("1")
    assertNull(result)
  }

  @Test
  fun userGroups_preservesGroupProperties() = runTest {
    val testGroup =
        Group(
            id = "test-123",
            name = "Test Group",
            description = "A detailed description",
            ownerId = "owner123",
            memberIds = List(42) { "user$it" },
            eventIds = listOf("event1", "event2", "event3"))
    addGroupsViaReflection(listOf(testGroup))

    val groups = repository.userGroups()

    val retrievedGroup = groups.first()
    assertEquals("test-123", retrievedGroup.id)
    assertEquals("Test Group", retrievedGroup.name)
    assertEquals("A detailed description", retrievedGroup.description)
    assertEquals("owner123", retrievedGroup.ownerId)
    assertEquals(42, retrievedGroup.membersCount)
    assertEquals(42, retrievedGroup.memberIds.size)
    assertEquals(3, retrievedGroup.eventIds.size)
  }

  @Test
  fun userGroups_withSpecialCharacters_preservesCorrectly() = runTest {
    val testGroups =
        listOf(
            Group(
                id = "1",
                name = "Café & Restaurant",
                ownerId = "owner1",
                memberIds = List(25) { "user$it" }),
            Group(
                id = "2",
                name = "Chess Game",
                ownerId = "owner2",
                memberIds = List(10) { "user$it" }))
    addGroupsViaReflection(testGroups)

    val groups = repository.userGroups()

    assertEquals("Café & Restaurant", groups[0].name)
    assertEquals("Chess Game", groups[1].name)
  }

  @Test
  fun userGroups_withEmptyDescription_handlesCorrectly() = runTest {
    val testGroup =
        Group(
            id = "1",
            name = "No Description",
            description = "",
            ownerId = "owner1",
            memberIds = List(5) { "user$it" })
    addGroupsViaReflection(listOf(testGroup))

    val groups = repository.userGroups()

    assertEquals("", groups.first().description)
  }

  @Test
  fun userGroups_withZeroMembers_handlesCorrectly() = runTest {
    val testGroup =
        Group(id = "1", name = "Empty Group", ownerId = "owner1", memberIds = emptyList())
    addGroupsViaReflection(listOf(testGroup))

    val groups = repository.userGroups()

    assertEquals(0, groups.first().membersCount)
  }

  @Test
  fun leaveGroup_maintainsOrderOfRemainingGroups() = runTest {
    val testGroups =
        listOf(
            Group(id = "1", name = "First", ownerId = "owner1"),
            Group(id = "2", name = "Second", ownerId = "owner2"),
            Group(id = "3", name = "Third", ownerId = "owner3"),
            Group(id = "4", name = "Fourth", ownerId = "owner4"))
    addGroupsViaReflection(testGroups)

    repository.leaveGroup("2")

    val groups = repository.userGroups()
    assertEquals(3, groups.size)
    assertEquals("First", groups[0].name)
    assertEquals("Third", groups[1].name)
    assertEquals("Fourth", groups[2].name)
  }

  // =======================================
  // Create Group Tests
  // =======================================

  @Test
  fun createGroup_successfullyCreatesAndReturnsGroupId() = runTest {
    val groupId =
        repository.createGroup(
            name = "Test Group", category = EventType.SOCIAL, description = "Test description")

    assertNotNull(groupId)
    assertTrue(groupId.isNotEmpty())
  }

  @Test
  fun createGroup_addsGroupToRepository() = runTest {
    val groupId =
        repository.createGroup(
            name = "New Group", category = EventType.ACTIVITY, description = "Description")

    val groups = repository.userGroups()
    assertEquals(1, groups.size)
    assertEquals(groupId, groups[0].id)
  }

  @Test
  fun createGroup_storesCorrectGroupData() = runTest {
    val groupId =
        repository.createGroup(
            name = "Sports Team", category = EventType.SPORTS, description = "Weekly games")

    val group = repository.getGroup(groupId)

    assertNotNull(group)
    assertEquals("Sports Team", group?.name)
    assertEquals(EventType.SPORTS, group?.category)
    assertEquals("Weekly games", group?.description)
    assertNotNull(group?.ownerId)
    assertTrue(group?.memberIds?.isNotEmpty() == true)
  }

  @Test
  fun createGroup_withEmptyDescription_createsSuccessfully() = runTest {
    val groupId =
        repository.createGroup(
            name = "Minimal Group", category = EventType.ACTIVITY, description = "")

    val group = repository.getGroup(groupId)

    assertNotNull(group)
    assertEquals("", group?.description)
  }

  @Test
  fun createGroup_withAllCategories_storesCorrectly() = runTest {
    val socialId =
        repository.createGroup(
            name = "Social Group", category = EventType.SOCIAL, description = "Social activities")

    val activityId =
        repository.createGroup(
            name = "Activity Group", category = EventType.ACTIVITY, description = "Fun activities")

    val sportsId =
        repository.createGroup(
            name = "Sports Group", category = EventType.SPORTS, description = "Sports events")

    val socialGroup = repository.getGroup(socialId)
    val activityGroup = repository.getGroup(activityId)
    val sportsGroup = repository.getGroup(sportsId)

    assertEquals(EventType.SOCIAL, socialGroup?.category)
    assertEquals(EventType.ACTIVITY, activityGroup?.category)
    assertEquals(EventType.SPORTS, sportsGroup?.category)
  }

  @Test
  fun createGroup_multipleGroups_allStoredCorrectly() = runTest {
    repository.createGroup("Group 1", EventType.SOCIAL, "Desc 1")
    repository.createGroup("Group 2", EventType.ACTIVITY, "Desc 2")
    repository.createGroup("Group 3", EventType.SPORTS, "Desc 3")

    val groups = repository.userGroups()

    assertEquals(3, groups.size)
    assertEquals("Group 1", groups[0].name)
    assertEquals("Group 2", groups[1].name)
    assertEquals("Group 3", groups[2].name)
  }

  @Test
  fun createGroup_generatesUniqueIds() = runTest {
    val id1 = repository.createGroup("Group 1", EventType.SOCIAL, "Desc")
    val id2 = repository.createGroup("Group 2", EventType.ACTIVITY, "Desc")
    val id3 = repository.createGroup("Group 3", EventType.SPORTS, "Desc")

    assertNotEquals(id1, id2)
    assertNotEquals(id2, id3)
    assertNotEquals(id1, id3)
  }

  @Test
  fun createGroup_afterCreating_canBeRetrievedByGetGroup() = runTest {
    val groupId =
        repository.createGroup(
            name = "Retrievable Group",
            category = EventType.ACTIVITY,
            description = "Can be retrieved")

    val retrievedGroup = repository.getGroup(groupId)

    assertNotNull(retrievedGroup)
    assertEquals(groupId, retrievedGroup?.id)
    assertEquals("Retrievable Group", retrievedGroup?.name)
  }

  @Test
  fun createGroup_afterCreating_appearsInUserGroups() = runTest {
    repository.createGroup("Group A", EventType.SOCIAL, "A")
    repository.createGroup("Group B", EventType.ACTIVITY, "B")

    val groups = repository.userGroups()

    assertEquals(2, groups.size)
    assertTrue(groups.any { it.name == "Group A" })
    assertTrue(groups.any { it.name == "Group B" })
  }

  @Test
  fun createGroup_createdGroupCanBeLeft() = runTest {
    val groupId =
        repository.createGroup(
            name = "Leavable Group", category = EventType.SPORTS, description = "Can be left")

    repository.leaveGroup(groupId)

    val groups = repository.userGroups()
    assertTrue(groups.isEmpty())
  }

  /**
   * Helper method to add groups to the repository using reflection. This is necessary because the
   * repository doesn't have a public method to add groups directly.
   */
  private fun addGroupsViaReflection(groups: List<Group>) {
    val field = GroupRepositoryLocal::class.java.getDeclaredField("groups")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST") val groupsList = field.get(repository) as MutableList<Group>
    groupsList.addAll(groups)
  }
}
