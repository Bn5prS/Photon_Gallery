package com.inferno.gallery.ui

import androidx.lifecycle.Lifecycle
import com.inferno.gallery.ui.utils.isEntrySafeToPop
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationExtensionsTest {

    @Test
    fun testIsEntrySafeToPop_onlyPermitsResumedState() {
        assertFalse("DESTROYED entry must not be popped", isEntrySafeToPop(Lifecycle.State.DESTROYED))
        assertFalse("INITIALIZED entry must not be popped", isEntrySafeToPop(Lifecycle.State.INITIALIZED))
        assertFalse("CREATED entry must not be popped", isEntrySafeToPop(Lifecycle.State.CREATED))
        assertFalse("STARTED entry must not be popped (transition in progress)", isEntrySafeToPop(Lifecycle.State.STARTED))
        assertTrue("RESUMED entry is safe to pop", isEntrySafeToPop(Lifecycle.State.RESUMED))
    }
}
