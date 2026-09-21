package com.prasjaychi.plantsense.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

/**
 * Standard Android Jetpack Compose helper to obtain a [ViewModel] scoped to a parent navigation graph.
 *
 * When navigating between multiple destinations in a nested graph (e.g. step 1 -> step 2 -> step 3),
 * this retrieves or creates the ViewModel on the parent graph's [NavBackStackEntry].
 * This guarantees that:
 * 1. All sub-destinations share the exact same ViewModel instance and state.
 * 2. When the user finishes or pops out of the nested graph, the ViewModel is automatically cleared.
 */
@Composable
inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(
    navController: NavController,
    parentGraphRoute: String? = null
): T {
    val route = parentGraphRoute ?: destination.parent?.route ?: throw IllegalStateException(
        "No parent navigation graph found for destination '${destination.route}'. Provide parentGraphRoute explicitly."
    )
    val parentEntry = remember(this) {
        navController.getBackStackEntry(route)
    }
    return viewModel(parentEntry)
}
