package com.inferno.gallery.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTransitionsTest {
    @Test
    fun testTabRouteIndices() {
        assertEquals(0, getMainTabRouteIndex("photos"))
        assertEquals(1, getMainTabRouteIndex("videos"))
        assertEquals(2, getMainTabRouteIndex("albums"))
        assertEquals(3, getMainTabRouteIndex("collections"))
        assertEquals(4, getMainTabRouteIndex("search"))
        assertEquals(-1, getMainTabRouteIndex("settings"))
        assertEquals(-1, getMainTabRouteIndex(null))
    }

    @Test
    fun testTransitionsNotNull() {
        val forwardEnter = getEnterTransition("photos", "albums")
        val forwardExit = getExitTransition("photos", "albums")
        val backwardEnter = getEnterTransition("albums", "photos")
        val backwardExit = getExitTransition("albums", "photos")
        val sameTabEnter = getEnterTransition("photos", "photos")
        val sameTabExit = getExitTransition("photos", "photos")

        assertNotNull(forwardEnter)
        assertNotNull(forwardExit)
        assertNotNull(backwardEnter)
        assertNotNull(backwardExit)
        assertNotNull(sameTabEnter)
        assertNotNull(sameTabExit)

        val subEnter = subScreenEnterTransition()
        val subExit = subScreenExitTransition()
        val subPopEnter = subScreenPopEnterTransition()
        val subPopExit = subScreenPopExitTransition()

        assertNotNull(subEnter)
        assertNotNull(subExit)
        assertNotNull(subPopEnter)
        assertNotNull(subPopExit)
    }
}
