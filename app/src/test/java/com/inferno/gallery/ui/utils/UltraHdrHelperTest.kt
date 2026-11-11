package com.inferno.gallery.ui.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class UltraHdrHelperTest {

    private open class TestContextWrapper(private val base: Context?) : ContextWrapper(base) {
        override fun getBaseContext(): Context? = base
    }

    @Test
    fun testHasGainmap_withNullBitmap_returnsFalse() {
        assertFalse(UltraHdrHelper.hasGainmap(null))
    }

    @Test
    fun testSetWindowHdrMode_withNullWindow_doesNotThrow() {
        // Safe null handling
        UltraHdrHelper.setWindowHdrMode(null, true)
        UltraHdrHelper.setWindowHdrMode(null, false)
    }

    @Test
    fun testFindActivity_withNullBaseContextWrapper_returnsNull() {
        val wrapper = TestContextWrapper(null)
        assertNull(UltraHdrHelper.findActivity(wrapper))
    }

    @Test
    fun testFindActivity_withNestedActivityWrapper_unwrapsActivity() {
        val activity = Activity()
        val wrapper1 = TestContextWrapper(activity)
        val wrapper2 = TestContextWrapper(wrapper1)
        assertEquals(activity, UltraHdrHelper.findActivity(wrapper2))
    }
}



