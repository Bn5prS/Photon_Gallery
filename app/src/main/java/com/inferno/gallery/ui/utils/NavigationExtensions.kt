package com.inferno.gallery.ui.utils

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

private const val TAG = "NavExtensions"

/**
 * Returns true if an entry in [state] is safe to pop without triggering
 * illegal transition states in the back stack.
 */
fun isEntrySafeToPop(state: Lifecycle.State): Boolean {
    return state.isAtLeast(Lifecycle.State.RESUMED)
}

/**
 * Safely pops the back stack of this [NavController].
 *
 * Prevents `java.lang.IllegalStateException: Cannot transition entry that is not in the back stack`
 * which occurs when:
 * 1. An entry is popped multiple times rapidly (e.g. concurrent deletion callbacks / double back presses).
 * 2. An entry's transition is invoked while it is not in the [Lifecycle.State.RESUMED] state.
 * 3. Predictive back gesture races against programmatic pops.
 *
 * @param from The specific [NavBackStackEntry] expected to be on top. If provided and not in [Lifecycle.State.RESUMED], the pop is rejected.
 * @return True if [NavController.popBackStack] was called and succeeded, false otherwise.
 */
fun NavController.popBackStackSafely(from: NavBackStackEntry? = null): Boolean {
    if (from != null && !isEntrySafeToPop(from.lifecycle.currentState)) {
        Log.w(TAG, "Skipping popBackStack: target entry ${from.destination.route} is in state ${from.lifecycle.currentState}, not RESUMED")
        return false
    }
    val current = currentBackStackEntry
    if (current == null) {
        Log.w(TAG, "Skipping popBackStack: currentBackStackEntry is null")
        return false
    }
    if (!isEntrySafeToPop(current.lifecycle.currentState)) {
        Log.w(TAG, "Skipping popBackStack: current entry ${current.destination.route} is in state ${current.lifecycle.currentState}, not RESUMED")
        return false
    }
    return try {
        popBackStack()
    } catch (e: Exception) {
        Log.e(TAG, "Caught exception during popBackStack: ${e.message}", e)
        false
    }
}
