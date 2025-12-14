package com.android.joinme.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Creates a [ViewModelProvider.Factory] for a [ViewModel] of type [T].
 *
 * This utility function simplifies the creation of ViewModel factories by providing a concise
 * inline syntax. It uses reified type parameters to create factories without verbose anonymous
 * object syntax.
 *
 * Usage:
 * ```
 * val viewModel: MyViewModel = viewModel(
 *     factory = createViewModelFactory { MyViewModel(repository) }
 * )
 * ```
 *
 * @param T The type of ViewModel to create
 * @param create A lambda that creates an instance of the ViewModel
 * @return A [ViewModelProvider.Factory] that creates instances of [T]
 */
inline fun <reified T : ViewModel> createViewModelFactory(
    crossinline create: () -> T
): ViewModelProvider.Factory {
  return object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
      return create() as VM
    }
  }
}
