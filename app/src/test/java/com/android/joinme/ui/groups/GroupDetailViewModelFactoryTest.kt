package com.android.joinme.ui.groups

import androidx.lifecycle.ViewModel
import com.android.joinme.model.groups.GroupRepository
import com.android.joinme.model.profile.ProfileRepository
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GroupDetailViewModelFactoryTest {

  private lateinit var mockGroupRepository: GroupRepository
  private lateinit var mockProfileRepository: ProfileRepository
  private lateinit var factory: GroupDetailViewModelFactory

  @Before
  fun setup() {
    mockGroupRepository = mockk(relaxed = true)
    mockProfileRepository = mockk(relaxed = true)
    factory = GroupDetailViewModelFactory(mockGroupRepository, mockProfileRepository)
  }

  @Test
  fun `create returns GroupDetailViewModel when correct class is provided`() {
    // When
    val viewModel = factory.create(GroupDetailViewModel::class.java)

    // Then
    assertNotNull(viewModel)
    assertTrue(viewModel is GroupDetailViewModel)
  }

  @Test
  fun `create throws IllegalArgumentException when unknown ViewModel class is provided`() {
    // Given
    class UnknownViewModel : ViewModel()

    // When & Then
    val exception =
        assertThrows(IllegalArgumentException::class.java) {
          factory.create(UnknownViewModel::class.java)
        }

    assertTrue(exception.message?.contains("Unknown ViewModel class") == true)
    assertTrue(exception.message?.contains(UnknownViewModel::class.java.name) == true)
  }

  @Test
  fun `factory can be instantiated with custom repositories`() {
    // Given
    val customGroupRepo = mockk<GroupRepository>(relaxed = true)
    val customProfileRepo = mockk<ProfileRepository>(relaxed = true)

    // When
    val customFactory = GroupDetailViewModelFactory(customGroupRepo, customProfileRepo)

    // Then
    assertNotNull(customFactory)
    val viewModel = customFactory.create(GroupDetailViewModel::class.java)
    assertNotNull(viewModel)
  }

  @Test
  fun `create returns new instance on each call`() {
    // When
    val viewModel1 = factory.create(GroupDetailViewModel::class.java)
    val viewModel2 = factory.create(GroupDetailViewModel::class.java)

    // Then
    assertNotNull(viewModel1)
    assertNotNull(viewModel2)
    assertNotSame(viewModel1, viewModel2)
  }
}
