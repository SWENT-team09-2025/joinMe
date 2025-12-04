package com.android.joinme.model.groups

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.android.joinme.model.event.EventType
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class GroupRepositoryLocalTest {

  @get:Rule val tempFolder = TemporaryFolder()

  private lateinit var repo: GroupRepositoryLocal
  private lateinit var sampleGroup: Group

  @Before
  fun setup() {
    repo = GroupRepositoryLocal()
    sampleGroup =
        Group(
            id = "1",
            name = "Basketball Club",
            category = EventType.SPORTS,
            description = "Weekly basketball games",
            ownerId = "owner1",
            memberIds = listOf("owner1", "member1", "member2"),
            eventIds = emptyList(),
            serieIds = emptyList(),
            photoUrl = null)
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
      repo.deleteGroup("1", "owner1")
      val all = repo.getAllGroups()
      Assert.assertTrue(all.isEmpty())
    }
  }

  @Test(expected = Exception::class)
  fun deleteGroup_nonOwner_throwsException() {
    runBlocking {
      repo.addGroup(sampleGroup)
      repo.deleteGroup("1", "notTheOwner")
    }
  }

  // ---------------- JOIN/LEAVE GROUP ----------------

  @Test
  fun joinGroup_addsUserToMembers() {
    runBlocking {
      repo.addGroup(sampleGroup)
      repo.joinGroup("1", "newUser")
      val group = repo.getGroup("1")
      Assert.assertTrue(group.memberIds.contains("newUser"))
      Assert.assertEquals(4, group.memberIds.size)
    }
  }

  @Test(expected = Exception::class)
  fun joinGroup_alreadyMember_throwsException() {
    runBlocking {
      repo.addGroup(sampleGroup)
      repo.joinGroup("1", "member1")
    }
  }

  @Test
  fun leaveGroup_removesUserFromMembers() {
    runBlocking {
      repo.addGroup(sampleGroup)
      repo.leaveGroup("1", "member1")
      val group = repo.getGroup("1")
      Assert.assertFalse(group.memberIds.contains("member1"))
      Assert.assertEquals(2, group.memberIds.size)
    }
  }

  @Test(expected = Exception::class)
  fun leaveGroup_notMember_throwsException() {
    runBlocking {
      repo.addGroup(sampleGroup)
      repo.leaveGroup("1", "notAMember")
    }
  }

  // ---------------- ADDITIONAL TESTS ----------------

  @Test
  fun addMultipleGroups_storesAll() {
    runBlocking {
      val g1 = sampleGroup
      val g2 = sampleGroup.copy(id = "2", name = "Running Club")
      val g3 = sampleGroup.copy(id = "3", name = "Cycling Club")
      repo.addGroup(g1)
      repo.addGroup(g2)
      repo.addGroup(g3)

      val all = repo.getAllGroups()
      Assert.assertEquals(3, all.size)
      Assert.assertTrue(all.any { it.name == "Cycling Club" })
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

  @Test
  fun clear_removesAllGroupsAndResetsCounter() {
    runBlocking {
      repo.addGroup(sampleGroup)
      repo.addGroup(sampleGroup.copy(id = "2"))
      repo.getNewGroupId() // increment counter

      repo.clear()

      Assert.assertTrue(repo.getAllGroups().isEmpty())
      Assert.assertEquals("0", repo.getNewGroupId())
    }
  }

  // ---------------- GET COMMON GROUPS TESTS ----------------

  @Test
  fun getCommonGroups_returnsEmptyListWhenNoUserIds() {
    runBlocking {
      repo.addGroup(sampleGroup)
      val result = repo.getCommonGroups(emptyList())
      Assert.assertTrue(result.isEmpty())
    }
  }

  @Test
  fun getCommonGroups_returnsGroupsWithSingleUser() {
    runBlocking {
      val g1 = sampleGroup.copy(id = "1", memberIds = listOf("user1", "user2"))
      val g2 = sampleGroup.copy(id = "2", memberIds = listOf("user3"))
      val g3 = sampleGroup.copy(id = "3", memberIds = listOf("user1", "user3"))
      repo.addGroup(g1)
      repo.addGroup(g2)
      repo.addGroup(g3)

      val result = repo.getCommonGroups(listOf("user1"))

      Assert.assertEquals(2, result.size)
      Assert.assertTrue(result.any { it.id == "1" })
      Assert.assertTrue(result.any { it.id == "3" })
    }
  }

  @Test
  fun getCommonGroups_returnsGroupsWithMultipleUsers() {
    runBlocking {
      val g1 = sampleGroup.copy(id = "1", memberIds = listOf("user1", "user2", "user3"))
      val g2 = sampleGroup.copy(id = "2", memberIds = listOf("user1", "user2"))
      val g3 = sampleGroup.copy(id = "3", memberIds = listOf("user1", "user3"))
      repo.addGroup(g1)
      repo.addGroup(g2)
      repo.addGroup(g3)

      val result = repo.getCommonGroups(listOf("user1", "user2"))

      Assert.assertEquals(2, result.size)
      Assert.assertTrue(result.any { it.id == "1" })
      Assert.assertTrue(result.any { it.id == "2" })
    }
  }

  @Test
  fun getCommonGroups_returnsEmptyListWhenNoCommonGroups() {
    runBlocking {
      val g1 = sampleGroup.copy(id = "1", memberIds = listOf("user1"))
      val g2 = sampleGroup.copy(id = "2", memberIds = listOf("user2"))
      repo.addGroup(g1)
      repo.addGroup(g2)

      val result = repo.getCommonGroups(listOf("user1", "user2"))

      Assert.assertTrue(result.isEmpty())
    }
  }

  @Test
  fun getCommonGroups_requiresAllUsersToBeMembers() {
    runBlocking {
      val g1 = sampleGroup.copy(id = "1", memberIds = listOf("user1", "user2", "user3"))
      val g2 = sampleGroup.copy(id = "2", memberIds = listOf("user1", "user2"))
      val g3 = sampleGroup.copy(id = "3", memberIds = listOf("user2", "user3"))
      repo.addGroup(g1)
      repo.addGroup(g2)
      repo.addGroup(g3)

      val result = repo.getCommonGroups(listOf("user1", "user2", "user3"))

      Assert.assertEquals(1, result.size)
      Assert.assertEquals("1", result[0].id)
    }
  }

  @Test
  fun getCommonGroups_worksWithLargeNumberOfUsers() {
    runBlocking {
      val users = (1..10).map { "user$it" }
      val g1 = sampleGroup.copy(id = "1", memberIds = users)
      val g2 = sampleGroup.copy(id = "2", memberIds = users.take(5))
      repo.addGroup(g1)
      repo.addGroup(g2)

      val result = repo.getCommonGroups(users)

      Assert.assertEquals(1, result.size)
      Assert.assertEquals("1", result[0].id)
    }
  }

  // ---------------- GROUP PHOTO TESTS ----------------

  @Test
  fun uploadGroupPhoto_updatesPhotoUrl() {
    runBlocking {
      // Setup
      repo.addGroup(sampleGroup)
      val mockContext = mock(Context::class.java)
      val mockContentResolver = mock(ContentResolver::class.java)
      val mockUri = mock(Uri::class.java)
      val filesDir = tempFolder.newFolder("files")
      val testBytes = "test image data".toByteArray()

      `when`(mockContext.filesDir).thenReturn(filesDir)
      `when`(mockContext.contentResolver).thenReturn(mockContentResolver)
      `when`(mockContentResolver.openInputStream(mockUri))
          .thenReturn(ByteArrayInputStream(testBytes))

      // Execute
      val result = repo.uploadGroupPhoto(mockContext, "1", mockUri)

      // Verify
      Assert.assertNotNull(result)
      Assert.assertTrue(result.startsWith("file:"))
      val group = repo.getGroup("1")
      Assert.assertEquals(result, group.photoUrl)
    }
  }

  @Test(expected = IllegalArgumentException::class)
  fun uploadGroupPhoto_groupNotFound_throwsException() {
    runBlocking {
      val mockContext = mock(Context::class.java)
      val mockUri = mock(Uri::class.java)
      repo.uploadGroupPhoto(mockContext, "nonexistent", mockUri)
    }
  }

  @Test
  fun deleteGroupPhoto_clearsPhotoUrl() {
    runBlocking {
      // Setup: add group with photo
      val groupWithPhoto = sampleGroup.copy(photoUrl = "file:///some/path/photo.jpg")
      repo.addGroup(groupWithPhoto)

      // Execute
      repo.deleteGroupPhoto("1")

      // Verify
      val group = repo.getGroup("1")
      Assert.assertNull(group.photoUrl)
    }
  }

  @Test
  fun deleteGroupPhoto_groupNotFound_doesNotThrow() {
    runBlocking {
      // Should not throw, just return silently
      repo.deleteGroupPhoto("nonexistent")
    }
  }

  @Test
  fun deleteGroupPhoto_noExistingPhoto_doesNotThrow() {
    runBlocking {
      // Setup: group without photo
      repo.addGroup(sampleGroup)

      // Execute - should not throw
      repo.deleteGroupPhoto("1")

      // Verify
      val group = repo.getGroup("1")
      Assert.assertNull(group.photoUrl)
    }
  }

  @Test
  fun uploadGroupPhoto_replacesExistingPhoto() {
    runBlocking {
      // Setup
      val groupWithPhoto = sampleGroup.copy(photoUrl = "file:///old/photo.jpg")
      repo.addGroup(groupWithPhoto)

      val mockContext = mock(Context::class.java)
      val mockContentResolver = mock(ContentResolver::class.java)
      val mockUri = mock(Uri::class.java)
      val filesDir = tempFolder.newFolder("files2")
      val testBytes = "new image data".toByteArray()

      `when`(mockContext.filesDir).thenReturn(filesDir)
      `when`(mockContext.contentResolver).thenReturn(mockContentResolver)
      `when`(mockContentResolver.openInputStream(mockUri))
          .thenReturn(ByteArrayInputStream(testBytes))

      // Execute
      val newUrl = repo.uploadGroupPhoto(mockContext, "1", mockUri)

      // Verify
      val group = repo.getGroup("1")
      Assert.assertEquals(newUrl, group.photoUrl)
      Assert.assertNotEquals("file:///old/photo.jpg", group.photoUrl)
    }
  }
}
