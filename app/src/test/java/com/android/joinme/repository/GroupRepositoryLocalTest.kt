package com.android.joinme.repository

import com.android.joinme.model.group.Group
import com.android.joinme.model.group.GroupRepositoryLocal
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class GroupRepositoryLocalTest {

  private lateinit var repo: GroupRepositoryLocal
  private lateinit var sampleGroup: Group

  @Before
  fun setup() {
    repo = GroupRepositoryLocal()
    sampleGroup =
        Group(
            id = "1",
            name = "Test Group",
            description = "A test group",
            ownerId = "user1",
            memberIds = listOf("A", "B"),
            eventIds = listOf("event1", "event2"))
  }

  // ---------------- BASIC CRUD ----------------

  @Test
  fun addAndGetGroup_success() {
    runBlocking {
      repo.addGroup(sampleGroup)
      val group = repo.getGroup("1")
      Assert.assertEquals(sampleGroup.name, group.name)
    }
  }

  @Test(expected = Exception::class)
  fun getGroup_notFound_throwsException() {
    runBlocking { repo.getGroup("unknown") }
  }

  @Test
  fun editGroup_updatesSuccessfully() {
    runBlocking {
      repo.addGroup(sampleGroup)
      val updated = sampleGroup.copy(name = "Updated Name")
      repo.editGroup("1", updated)
      val group = repo.getGroup("1")
      Assert.assertEquals("Updated Name", group.name)
    }
  }

  @Test
  fun deleteGroup_removesSuccessfully() {
    runBlocking {
      repo.addGroup(sampleGroup)
      repo.deleteGroup("1")
      val all = repo.getAllGroups()
      Assert.assertTrue(all.isEmpty())
    }
  }

  // ---------------- ADDITIONAL TESTS ----------------

  @Test
  fun addMultipleGroups_storesAll() {
    runBlocking {
      val g1 = sampleGroup
      val g2 = sampleGroup.copy(id = "2", name = "Group 2")
      val g3 = sampleGroup.copy(id = "3", name = "Group 3")
      repo.addGroup(g1)
      repo.addGroup(g2)
      repo.addGroup(g3)

      val all = repo.getAllGroups()
      Assert.assertEquals(3, all.size)
      Assert.assertTrue(all.any { it.name == "Group 3" })
    }
  }

  @Test
  fun getNewGroupId_incrementsSequentially() {
    val id1 = repo.getNewGroupId()
    val id2 = repo.getNewGroupId()
    val id3 = repo.getNewGroupId()
    Assert.assertEquals((id1.toInt() + 1).toString(), id2)
    Assert.assertEquals((id2.toInt() + 1).toString(), id3)
  }

  @Test(expected = Exception::class)
  fun editGroup_notFound_throwsException() {
    runBlocking {
      val fake = sampleGroup.copy(id = "999", name = "DoesNotExist")
      repo.editGroup(fake.id, fake)
    }
  }

  @Test(expected = Exception::class)
  fun deleteGroup_notFound_throwsException() {
    runBlocking {
      repo.addGroup(sampleGroup)
      repo.deleteGroup("nonexistent")
    }
  }

  @Test
  fun addDuplicateId_keepsOriginalOnGet() {
    runBlocking {
      repo.addGroup(sampleGroup)
      val duplicate = sampleGroup.copy(name = "Duplicate Group")
      repo.addGroup(duplicate)

      // getGroup returns the first matching item (original)
      val fetched = repo.getGroup("1")
      Assert.assertEquals("Test Group", fetched.name)

      // and both entries exist with the same ID
      val allWithSameId = repo.getAllGroups().filter { it.id == "1" }
      Assert.assertEquals(2, allWithSameId.size)
    }
  }

  @Test
  fun getAllGroups_initiallyReturnsEmptyList() {
    runBlocking {
      val groups = repo.getAllGroups()
      Assert.assertTrue(groups.isEmpty())
    }
  }

  @Test
  fun getGroup_preservesAllProperties() {
    runBlocking {
      repo.addGroup(sampleGroup)
      val retrieved = repo.getGroup("1")
      Assert.assertEquals(sampleGroup.id, retrieved.id)
      Assert.assertEquals(sampleGroup.name, retrieved.name)
      Assert.assertEquals(sampleGroup.description, retrieved.description)
      Assert.assertEquals(sampleGroup.ownerId, retrieved.ownerId)
      Assert.assertEquals(sampleGroup.memberIds, retrieved.memberIds)
      Assert.assertEquals(sampleGroup.eventIds, retrieved.eventIds)
    }
  }
}
