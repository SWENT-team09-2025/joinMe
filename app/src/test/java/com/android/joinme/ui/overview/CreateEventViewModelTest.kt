package com.android.joinme.ui.overview

import androidx.test.core.app.ApplicationProvider
import com.android.joinme.model.event.Event
import com.android.joinme.model.event.EventFilter
import com.android.joinme.model.event.EventsRepository
import com.android.joinme.model.groups.Group
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.map.Location
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Note: This file was co-written with AI (Claude) */

/**
 * Unit tests for CreateEventViewModel.
 *
 * These tests avoid asserting on exact error strings (which can change), and instead assert on
 * invalid* flags, isValid, and repository interactions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class CreateEventViewModelTest {

  // ---- Simple fake repo that records added events ----
  private class FakeEventsRepository : EventsRepository {
    val added = mutableListOf<Event>()

    override suspend fun addEvent(event: Event) {
      added += event
    }

    override suspend fun editEvent(eventId: String, newValue: Event) {
      /* no-op */
    }

    override suspend fun deleteEvent(eventId: String) {
      /* no-op */
    }

    override suspend fun getEventsByIds(eventIds: List<String>): List<Event> {
      /* no-op */
      return emptyList()
    }

    override suspend fun getEvent(eventId: String): Event =
        added.find { it.eventId == eventId } ?: throw NoSuchElementException("Event not found")

    override suspend fun getAllEvents(eventFilter: EventFilter): List<Event> = added.toList()

    override fun getNewEventId(): String = "fake-id-1"
  }

  // ---- Simple fake group repo that records groups and edits ----
  private class FakeGroupRepository : GroupRepository {
    private val groups = mutableMapOf<String, Group>()
    var shouldThrowOnGet = false
    var shouldThrowOnEdit = false
    var shouldThrowOnGetAll = false

    fun addTestGroup(group: Group) {
      groups[group.id] = group
    }

    override fun getNewGroupId(): String = "fake-group-id"

    override suspend fun getAllGroups(): List<Group> {
      if (shouldThrowOnGetAll) throw Exception("Failed to get all groups")
      return groups.values.toList()
    }

    override suspend fun getGroup(groupId: String): Group {
      if (shouldThrowOnGet) throw Exception("Failed to get group")
      return groups[groupId] ?: throw Exception("Group not found")
    }

    override suspend fun addGroup(group: Group) {
      groups[group.id] = group
    }

    override suspend fun editGroup(groupId: String, newValue: Group) {
      if (shouldThrowOnEdit) throw Exception("Failed to edit group")
      groups[groupId] = newValue
    }

    override suspend fun deleteGroup(groupId: String, userId: String) {
      groups.remove(groupId)
    }

    override suspend fun leaveGroup(groupId: String, userId: String) {
      /* no-op */
    }

    override suspend fun joinGroup(groupId: String, userId: String) {
      /* no-op */
    }
  }

  private lateinit var repo: FakeEventsRepository
  private lateinit var groupRepo: FakeGroupRepository
  private lateinit var vm: CreateEventViewModel
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    // Initialize Firebase for tests that call createEvent()
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }

    Dispatchers.setMain(testDispatcher)
    repo = FakeEventsRepository()
    groupRepo = FakeGroupRepository()
    vm = CreateEventViewModel(repo, groupRepo)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ---------- Basic validity ----------

  @Test
  fun initialState_isInvalid() {
    val s = vm.uiState.value
    Assert.assertFalse(s.isValid)
  }

  @Test
  fun fillingAllFields_makesFormValid() {
    vm.setType("SPORTS")
    vm.setTitle("Football")
    vm.setDescription("Friendly 5v5")
    vm.selectLocation(Location(46.52, 6.63, "EPFL Field"))
    vm.setDate("25/12/2023")
    vm.setTime("10:00")
    vm.setMaxParticipants("10")
    vm.setDuration("90")
    vm.setVisibility("PUBLIC")

    Assert.assertTrue(vm.uiState.value.isValid)
  }

  // ---------- Field validation edges ----------

  @Test
  fun setType_invalid_marksInvalid() {
    vm.setType("MUSIC")
    val s = vm.uiState.value
    Assert.assertNotNull(s.invalidTypeMsg)
    Assert.assertFalse(s.isValid)
  }

  @Test
  fun setVisibility_invalid_marksInvalid() {
    vm.setVisibility("PUBLICK")
    val s = vm.uiState.value
    Assert.assertNotNull(s.invalidVisibilityMsg)
    Assert.assertFalse(s.isValid)
  }

  @Test
  fun setDate_wrongFormat_marksInvalid() {
    vm.setDate("2023-12-25 10:00")
    val s = vm.uiState.value
    Assert.assertNotNull(s.invalidDateMsg)
    Assert.assertFalse(s.isValid)
  }

  @Test
  fun setMaxParticipants_invalidValues_marksInvalid() {
    // Test non-numeric value
    vm.setMaxParticipants("ten")
    var s = vm.uiState.value
    Assert.assertNotNull(s.invalidMaxParticipantsMsg)
    Assert.assertFalse(s.isValid)

    // Test negative value
    vm.setMaxParticipants("-1")
    s = vm.uiState.value
    Assert.assertNotNull(s.invalidMaxParticipantsMsg)
    Assert.assertFalse(s.isValid)
  }

  @Test
  fun setDuration_invalidValues_marksInvalid() {
    // Test non-numeric value
    vm.setDuration("abc")
    var s = vm.uiState.value
    Assert.assertNotNull(s.invalidDurationMsg)
    Assert.assertFalse(s.isValid)

    // Test zero value
    vm.setDuration("0")
    s = vm.uiState.value
    Assert.assertNotNull(s.invalidDurationMsg)
    Assert.assertFalse(s.isValid)
  }

  // ---------- createEvent() behavior ----------

  @Test
  fun createEvent_withInvalidForm_returnsFalse_andDoesNotAdd() = runTest {
    // leave blank => invalid
    val ok = vm.createEvent()
    advanceUntilIdle()

    Assert.assertFalse(ok)
    Assert.assertTrue(repo.added.isEmpty())
    Assert.assertNotNull(vm.uiState.value.errorMsg) // some error is surfaced
  }

  @Test
  fun createEvent_withInvalidDate_returnsFalse_andDoesNotAdd() = runTest {
    vm.setType("SPORTS")
    vm.setTitle("Basketball")
    vm.setDescription("3v3")
    vm.setLocation("EPFL Gym")
    vm.setMaxParticipants("6")
    vm.setDuration("60")
    vm.setVisibility("PUBLIC")
    vm.setDate("not a date")

    val ok = vm.createEvent()
    advanceUntilIdle()

    Assert.assertFalse(ok)
    Assert.assertTrue(repo.added.isEmpty())
    Assert.assertNotNull(vm.uiState.value.errorMsg)
  }

  // ---------- error clearing ----------

  @Test
  fun clearErrorMsg_resetsErrorField() = runTest {
    // trigger an error
    val ok = vm.createEvent()
    Assert.assertFalse(ok)
    Assert.assertNotNull(vm.uiState.value.errorMsg)

    vm.clearErrorMsg()
    Assert.assertNull(vm.uiState.value.errorMsg)
  }

  // ---------- group loading ----------

  @Test
  fun initialState_loadsAvailableGroups() = runTest {
    // Initially should be empty
    val emptyVm = CreateEventViewModel(repo, groupRepo)
    advanceUntilIdle()
    Assert.assertTrue(emptyVm.uiState.value.availableGroups.isEmpty())

    // Add test groups and create new VM
    val group1 = Group(id = "group-1", name = "Group 1")
    val group2 = Group(id = "group-2", name = "Group 2")
    groupRepo.addTestGroup(group1)
    groupRepo.addTestGroup(group2)

    val newVm = CreateEventViewModel(repo, groupRepo)
    advanceUntilIdle()

    Assert.assertEquals(2, newVm.uiState.value.availableGroups.size)
    Assert.assertTrue(newVm.uiState.value.availableGroups.contains(group1))
    Assert.assertTrue(newVm.uiState.value.availableGroups.contains(group2))
  }

  // ---------- group selection ----------

  @Test
  fun setSelectedGroup_updatesStateAndCanBeCleared() = runTest {
    // Initially should have no selected group
    Assert.assertNull(vm.uiState.value.selectedGroupId)

    // Add a test group
    val testGroup = Group(id = "group-123", name = "Test Group")
    groupRepo.addTestGroup(testGroup)

    val newVm = CreateEventViewModel(repo, groupRepo)
    advanceUntilIdle()

    // Initially should still be null
    Assert.assertNull(newVm.uiState.value.selectedGroupId)

    // Select the group
    newVm.setSelectedGroup("group-123")
    Assert.assertEquals("group-123", newVm.uiState.value.selectedGroupId)

    // Clear the selection
    newVm.setSelectedGroup(null)
    Assert.assertNull(newVm.uiState.value.selectedGroupId)
  }

  @Test
  fun setMaxParticipants_whenGroupSelected_validatesAgainstGroupSize() = runTest {
    // Set up a group with 5 members
    val testGroup =
        Group(id = "group-1", name = "Test Group", memberIds = listOf("u1", "u2", "u3", "u4", "u5"))
    groupRepo.addTestGroup(testGroup)

    val newVm = CreateEventViewModel(repo, groupRepo)
    advanceUntilIdle()

    // Select the group
    newVm.setSelectedGroup("group-1")

    // Try to set maxParticipants less than group size
    newVm.setMaxParticipants("3")
    Assert.assertNotNull(newVm.uiState.value.invalidMaxParticipantsMsg)
    Assert.assertTrue(newVm.uiState.value.invalidMaxParticipantsMsg!!.contains("at least 5"))

    // Set maxParticipants equal to group size
    newVm.setMaxParticipants("5")
    Assert.assertNull(newVm.uiState.value.invalidMaxParticipantsMsg)

    // Set maxParticipants greater than group size
    newVm.setMaxParticipants("10")
    Assert.assertNull(newVm.uiState.value.invalidMaxParticipantsMsg)
  }

  @Test
  fun setSelectedGroup_revalidatesMaxParticipants() = runTest {
    // Set up a group with 5 members
    val testGroup =
        Group(id = "group-1", name = "Test Group", memberIds = listOf("u1", "u2", "u3", "u4", "u5"))
    groupRepo.addTestGroup(testGroup)

    val newVm = CreateEventViewModel(repo, groupRepo)
    advanceUntilIdle()

    // Set maxParticipants to 3 (valid for standalone)
    newVm.setMaxParticipants("3")
    Assert.assertNull(newVm.uiState.value.invalidMaxParticipantsMsg)

    // Select group - auto-sets to 300, so validation passes
    newVm.setSelectedGroup("group-1")
    Assert.assertEquals("300", newVm.uiState.value.maxParticipants)
    Assert.assertNull(newVm.uiState.value.invalidMaxParticipantsMsg)

    // Clear group selection - resets to empty
    newVm.setSelectedGroup(null)
    Assert.assertEquals("", newVm.uiState.value.maxParticipants)
    Assert.assertNull(newVm.uiState.value.invalidMaxParticipantsMsg)
  }

  @Test
  fun setSelectedGroup_autoSetsTypeMaxParticipantsAndVisibility() = runTest {
    // Set up a SPORTS group with 3 members
    val testGroup =
        Group(
            id = "group-1",
            name = "Sports Group",
            category = com.android.joinme.model.event.EventType.SPORTS,
            memberIds = listOf("u1", "u2", "u3"))
    groupRepo.addTestGroup(testGroup)

    val newVm = CreateEventViewModel(repo, groupRepo)
    advanceUntilIdle()

    // Initially fields should be empty
    Assert.assertEquals("", newVm.uiState.value.type)
    Assert.assertEquals("", newVm.uiState.value.maxParticipants)
    Assert.assertEquals("", newVm.uiState.value.visibility)

    // Select group
    newVm.setSelectedGroup("group-1")

    // Verify auto-set fields
    Assert.assertEquals("SPORTS", newVm.uiState.value.type)
    Assert.assertEquals("300", newVm.uiState.value.maxParticipants)
    Assert.assertEquals("PRIVATE", newVm.uiState.value.visibility)

    // Verify validation messages are cleared
    Assert.assertNull(newVm.uiState.value.invalidTypeMsg)
    Assert.assertNull(newVm.uiState.value.invalidMaxParticipantsMsg)
    Assert.assertNull(newVm.uiState.value.invalidVisibilityMsg)
  }

  @Test
  fun setSelectedGroup_withDifferentGroupTypes_setsCorrectType() = runTest {
    // Set up groups with different types
    val sportsGroup =
        Group(
            id = "sports-group",
            name = "Sports",
            category = com.android.joinme.model.event.EventType.SPORTS)
    val socialGroup =
        Group(
            id = "social-group",
            name = "Social",
            category = com.android.joinme.model.event.EventType.SOCIAL)
    val activityGroup =
        Group(
            id = "activity-group",
            name = "Activity",
            category = com.android.joinme.model.event.EventType.ACTIVITY)

    groupRepo.addTestGroup(sportsGroup)
    groupRepo.addTestGroup(socialGroup)
    groupRepo.addTestGroup(activityGroup)

    val newVm = CreateEventViewModel(repo, groupRepo)
    advanceUntilIdle()

    // Test SPORTS group
    newVm.setSelectedGroup("sports-group")
    Assert.assertEquals("SPORTS", newVm.uiState.value.type)

    // Test SOCIAL group
    newVm.setSelectedGroup("social-group")
    Assert.assertEquals("SOCIAL", newVm.uiState.value.type)

    // Test ACTIVITY group
    newVm.setSelectedGroup("activity-group")
    Assert.assertEquals("ACTIVITY", newVm.uiState.value.type)
  }

  @Test
  fun setSelectedGroup_clearingGroup_resetsFieldsToEmpty() = runTest {
    // Set up a group
    val testGroup =
        Group(
            id = "group-1",
            name = "Test Group",
            category = com.android.joinme.model.event.EventType.SPORTS,
            memberIds = listOf("u1", "u2", "u3"))
    groupRepo.addTestGroup(testGroup)

    val newVm = CreateEventViewModel(repo, groupRepo)
    advanceUntilIdle()

    // Select group to set fields
    newVm.setSelectedGroup("group-1")
    Assert.assertEquals("SPORTS", newVm.uiState.value.type)
    Assert.assertEquals("300", newVm.uiState.value.maxParticipants)
    Assert.assertEquals("PRIVATE", newVm.uiState.value.visibility)

    // Clear group selection
    newVm.setSelectedGroup(null)

    // Verify fields are reset to empty
    Assert.assertEquals("", newVm.uiState.value.type)
    Assert.assertEquals("", newVm.uiState.value.maxParticipants)
    Assert.assertEquals("", newVm.uiState.value.visibility)

    // Verify validation messages are cleared
    Assert.assertNull(newVm.uiState.value.invalidTypeMsg)
    Assert.assertNull(newVm.uiState.value.invalidMaxParticipantsMsg)
    Assert.assertNull(newVm.uiState.value.invalidVisibilityMsg)
  }

  @Test
  fun setSelectedGroup_withManuallySetFields_thenClearingGroup_resetsFields() = runTest {
    // Manually set some fields first
    vm.setType("SPORTS")
    vm.setMaxParticipants("10")
    vm.setVisibility("PUBLIC")

    Assert.assertEquals("SPORTS", vm.uiState.value.type)
    Assert.assertEquals("10", vm.uiState.value.maxParticipants)
    Assert.assertEquals("PUBLIC", vm.uiState.value.visibility)

    // Set up and select a group
    val testGroup = Group(id = "group-1", name = "Test Group")
    groupRepo.addTestGroup(testGroup)

    val newVm = CreateEventViewModel(repo, groupRepo)
    advanceUntilIdle()

    // Set fields manually
    newVm.setType("ACTIVITY")
    newVm.setMaxParticipants("15")
    newVm.setVisibility("PRIVATE")

    // Select group - should override with group values
    newVm.setSelectedGroup("group-1")
    Assert.assertEquals("ACTIVITY", newVm.uiState.value.type) // Group's category
    Assert.assertEquals("300", newVm.uiState.value.maxParticipants)
    Assert.assertEquals("PRIVATE", newVm.uiState.value.visibility)

    // Clear group - should reset to empty
    newVm.setSelectedGroup(null)
    Assert.assertEquals("", newVm.uiState.value.type)
    Assert.assertEquals("", newVm.uiState.value.maxParticipants)
    Assert.assertEquals("", newVm.uiState.value.visibility)
  }

  // ---------- createEvent with group ----------

  @Test
  fun createEvent_withValidFormAndNoGroup_createsStandaloneEvent() = runTest {
    vm.setType("SPORTS")
    vm.setTitle("Football")
    vm.setDescription("Friendly 5v5")
    vm.selectLocation(Location(46.52, 6.63, "EPFL Field"))
    vm.setDate("25/12/2023")
    vm.setTime("10:00")
    vm.setMaxParticipants("10")
    vm.setDuration("90")
    vm.setVisibility("PUBLIC")

    val ok = vm.createEvent()
    advanceUntilIdle()

    Assert.assertTrue(ok)
    Assert.assertEquals(1, repo.added.size)
    // Ensure no group was modified
    Assert.assertTrue(groupRepo.getAllGroups().isEmpty())
  }

  @Test
  fun createEvent_withValidFormAndGroup_addsEventToGroupAndMembersAsParticipants() = runTest {
    // Set up a test group with members
    val testGroup =
        Group(
            id = "group-1",
            name = "Test Group",
            memberIds = listOf("user1", "user2", "user3"),
            eventIds = emptyList())
    groupRepo.addTestGroup(testGroup)

    // Create new VM with the group already loaded
    val newVm = CreateEventViewModel(repo, groupRepo)
    advanceUntilIdle()

    // Select group first (this will auto-set type, maxParticipants, visibility)
    newVm.setSelectedGroup("group-1")

    // Fill remaining form fields
    newVm.setTitle("Football")
    newVm.setDescription("Friendly 5v5")
    newVm.selectLocation(Location(46.52, 6.63, "EPFL Field"))
    newVm.setDate("25/12/2023")
    newVm.setTime("10:00")
    newVm.setDuration("90")

    val ok = newVm.createEvent()
    advanceUntilIdle()

    Assert.assertTrue(ok)
    Assert.assertEquals(1, repo.added.size)

    // Verify event has group members as participants
    val createdEvent = repo.added[0]
    Assert.assertEquals(3, createdEvent.participants.size)
    Assert.assertTrue(createdEvent.participants.containsAll(listOf("user1", "user2", "user3")))

    // Verify group was updated with event ID
    val updatedGroup = groupRepo.getGroup("group-1")
    Assert.assertEquals(1, updatedGroup.eventIds.size)
    Assert.assertEquals("fake-id-1", updatedGroup.eventIds[0])
  }

  @Test
  fun createEvent_withGroupNotFound_returnsFalseAndSetsError() = runTest {
    vm.setType("SPORTS")
    vm.setTitle("Football")
    vm.setDescription("Friendly 5v5")
    vm.selectLocation(Location(46.52, 6.63, "EPFL Field"))
    vm.setDate("25/12/2023")
    vm.setTime("10:00")
    vm.setMaxParticipants("10")
    vm.setDuration("90")
    vm.setVisibility("PUBLIC")
    vm.setSelectedGroup("non-existent-group")

    val ok = vm.createEvent()
    advanceUntilIdle()

    Assert.assertFalse(ok)
    Assert.assertNotNull(vm.uiState.value.errorMsg)
    Assert.assertEquals(0, repo.added.size) // Event was NOT created
  }

  @Test
  fun createEvent_withGroupEditFailure_returnsFalseAndSetsError() = runTest {
    val testGroup = Group(id = "group-1", name = "Test Group", eventIds = emptyList())
    groupRepo.addTestGroup(testGroup)
    groupRepo.shouldThrowOnEdit = true

    // Create new VM with the group already loaded
    val newVm = CreateEventViewModel(repo, groupRepo)
    advanceUntilIdle()

    // Select group first (this will auto-set type, maxParticipants, visibility)
    newVm.setSelectedGroup("group-1")

    // Fill remaining form fields
    newVm.setTitle("Football")
    newVm.setDescription("Friendly 5v5")
    newVm.selectLocation(Location(46.52, 6.63, "EPFL Field"))
    newVm.setDate("25/12/2023")
    newVm.setTime("10:00")
    newVm.setDuration("90")

    val ok = newVm.createEvent()
    advanceUntilIdle()

    Assert.assertFalse(ok)
    Assert.assertNotNull(newVm.uiState.value.errorMsg)
    Assert.assertEquals(1, repo.added.size) // Event was created
  }
}
