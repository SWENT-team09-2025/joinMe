package com.android.joinme.util

import androidx.lifecycle.ViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ViewModelFactoryHelperTest {

  /** Test ViewModel for verifying factory creation. */
  private class TestViewModel(val value: String) : ViewModel()

  @Test
  fun `createViewModelFactory creates factory that produces correct ViewModel`() {
    val expectedValue = "test_value"
    val factory = createViewModelFactory { TestViewModel(expectedValue) }

    val viewModel = factory.create(TestViewModel::class.java)

    assertNotNull(viewModel)
    assertEquals(expectedValue, viewModel.value)
  }

  @Test
  fun `createViewModelFactory invokes create lambda each time`() {
    var invocationCount = 0
    val factory = createViewModelFactory {
      invocationCount++
      TestViewModel("instance_$invocationCount")
    }

    val viewModel1 = factory.create(TestViewModel::class.java)
    val viewModel2 = factory.create(TestViewModel::class.java)

    assertEquals(2, invocationCount)
    assertEquals("instance_1", viewModel1.value)
    assertEquals("instance_2", viewModel2.value)
  }

  @Test
  fun `createViewModelFactory returns new instance each call`() {
    val factory = createViewModelFactory { TestViewModel("test") }

    val viewModel1 = factory.create(TestViewModel::class.java)
    val viewModel2 = factory.create(TestViewModel::class.java)

    assertNotNull(viewModel1)
    assertNotNull(viewModel2)
    assert(viewModel1 !== viewModel2) { "Factory should create new instances" }
  }

  @Test
  fun `createViewModelFactory works with ViewModel with no constructor args`() {
    class SimpleViewModel : ViewModel()

    val factory = createViewModelFactory { SimpleViewModel() }
    val viewModel = factory.create(SimpleViewModel::class.java)

    assertNotNull(viewModel)
  }

  @Test
  fun `createViewModelFactory preserves ViewModel with multiple dependencies`() {
    class ComplexViewModel(val dep1: String, val dep2: Int, val dep3: Boolean) : ViewModel()

    val factory = createViewModelFactory { ComplexViewModel("hello", 42, true) }
    val viewModel = factory.create(ComplexViewModel::class.java)

    assertEquals("hello", viewModel.dep1)
    assertEquals(42, viewModel.dep2)
    assertEquals(true, viewModel.dep3)
  }
}
