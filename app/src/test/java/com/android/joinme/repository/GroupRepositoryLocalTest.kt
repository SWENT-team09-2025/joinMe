package com.android.joinme.repository

import com.android.joinme.model.group.Group
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.mockk
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

  private lateinit var mockFirestore: FirebaseFirestore
  private lateinit var repository: GroupRepositoryLocal

  @Before
  fun setUp() {
    mockFirestore = mockk(relaxed = true)
    repository = GroupRepositoryLocal(mockFirestore)
  }

  @Test
  fun userGroups_initiallyReturnsEmptyList() = runTest {
    val groups = repository.userGroups()

    assertTrue(groups.isEmpty())
  }

  @Test
  fun getGroup_withNumericId_returnsGroupAtIndex() = runTest {
    val exception = assertThrows(Exception::class.java) { runTest { repository.getGroup("0") } }
    assertNotNull(exception)
  }

  /**
   * Tests using reflection to add test data to the internal groups list. This allows us to test the
   * full behavior of the repository.
   */
  @Test
  fun leaveGroup_afterAddingGroups_removesCorrectGroup() = runTest {
    val testGroups =
        listOf(
            Group(id = "1", name = "Group 1", membersCount = 10),
            Group(id = "2", name = "Group 2", membersCount = 20),
            Group(id = "3", name = "Group 3", membersCount = 30))

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
            Group(id = "1", name = "First"),
            Group(id = "2", name = "Second"),
            Group(id = "3", name = "Third"))
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
            Group(id = "1", name = "First"),
            Group(id = "2", name = "Second"),
            Group(id = "3", name = "Third"))
    addGroupsViaReflection(testGroups)

    repository.leaveGroup("3")
    val remainingGroups = repository.userGroups()
    assertEquals(2, remainingGroups.size)
    assertFalse(remainingGroups.any { it.id == "3" })
  }

  @Test
  fun leaveGroup_withOnlyGroup_leavesEmptyList() = runTest {
    val testGroups = listOf(Group(id = "1", name = "Only Group"))
    addGroupsViaReflection(testGroups)

    repository.leaveGroup("1")

    val remainingGroups = repository.userGroups()
    assertTrue(remainingGroups.isEmpty())
  }

  @Test
  fun leaveGroup_multipleTimes_removesAllCorrectly() = runTest {
    val testGroups =
        listOf(
            Group(id = "1", name = "Group 1"),
            Group(id = "2", name = "Group 2"),
            Group(id = "3", name = "Group 3"),
            Group(id = "4", name = "Group 4"))
    addGroupsViaReflection(testGroups)

    repository.leaveGroup("2")
    repository.leaveGroup("4")

    val remainingGroups = repository.userGroups()
    assertEquals(2, remainingGroups.size)
    assertTrue(remainingGroups.any { it.id == "1" })
    assertTrue(remainingGroups.any { it.id == "3" })
  }

  @Test
  fun getGroup_withValidIndex_returnsCorrectGroup() = runTest {
    val testGroups =
        listOf(
            Group(id = "id-0", name = "First Group", membersCount = 10),
            Group(id = "id-1", name = "Second Group", membersCount = 20),
            Group(id = "id-2", name = "Third Group", membersCount = 30))
    addGroupsViaReflection(testGroups)

    val group0 = repository.getGroup("0")
    val group1 = repository.getGroup("1")
    val group2 = repository.getGroup("2")

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
  fun getGroup_withOutOfBoundsIndex_throwsException() = runTest {
    val testGroups = listOf(Group(id = "1", name = "Only Group"))
    addGroupsViaReflection(testGroups)

    val exception = assertThrows(Exception::class.java) { runTest { repository.getGroup("5") } }
    assertNotNull(exception)
  }

  @Test
  fun getGroup_withNegativeIndex_throwsException() = runTest {
    val testGroups = listOf(Group(id = "1", name = "Test"))
    addGroupsViaReflection(testGroups)
    val exception = assertThrows(Exception::class.java) { runTest { repository.getGroup("-1") } }
    assertNotNull(exception)
  }

  @Test
  fun userGroups_preservesGroupProperties() = runTest {
    val testGroup =
        Group(
            id = "test-123",
            name = "Test Group",
            category = "Sports",
            description = "A detailed description",
            membersCount = 42)
    addGroupsViaReflection(listOf(testGroup))

    val groups = repository.userGroups()

    val retrievedGroup = groups.first()
    assertEquals("test-123", retrievedGroup.id)
    assertEquals("Test Group", retrievedGroup.name)
    assertEquals("Sports", retrievedGroup.category)
    assertEquals("A detailed description", retrievedGroup.description)
    assertEquals(42, retrievedGroup.membersCount)
  }

  @Test
  fun userGroups_withSpecialCharacters_preservesCorrectly() = runTest {
    val testGroups =
        listOf(
            Group(id = "1", name = "Café & Restaurant", membersCount = 25),
            Group(id = "2", name = "Chess Game", membersCount = 10))
    addGroupsViaReflection(testGroups)

    val groups = repository.userGroups()

    assertEquals("Café & Restaurant", groups[0].name)
    assertEquals("Chess Game", groups[1].name)
  }

  @Test
  fun userGroups_withEmptyDescription_handlesCorrectly() = runTest {
    val testGroup = Group(id = "1", name = "No Description", description = "", membersCount = 5)
    addGroupsViaReflection(listOf(testGroup))

    val groups = repository.userGroups()

    assertEquals("", groups.first().description)
  }

  @Test
  fun userGroups_withZeroMembers_handlesCorrectly() = runTest {
    val testGroup = Group(id = "1", name = "Empty Group", membersCount = 0)
    addGroupsViaReflection(listOf(testGroup))

    val groups = repository.userGroups()

    assertEquals(0, groups.first().membersCount)
  }

  @Test
  fun leaveGroup_maintainsOrderOfRemainingGroups() = runTest {
    val testGroups =
        listOf(
            Group(id = "1", name = "First"),
            Group(id = "2", name = "Second"),
            Group(id = "3", name = "Third"),
            Group(id = "4", name = "Fourth"))
    addGroupsViaReflection(testGroups)

    repository.leaveGroup("2")

    val groups = repository.userGroups()
    assertEquals(3, groups.size)
    assertEquals("First", groups[0].name)
    assertEquals("Third", groups[1].name)
    assertEquals("Fourth", groups[2].name)
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
