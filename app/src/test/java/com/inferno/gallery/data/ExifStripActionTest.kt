package com.inferno.gallery.data

import com.inferno.gallery.data.model.ExifStripAction
import org.junit.Assert.assertEquals
import org.junit.Test

class ExifStripActionTest {
    @Test
    fun testFromId_returnsCorrectActionOrDefault() {
        assertEquals(ExifStripAction.ASK_EVERY_TIME, ExifStripAction.fromId("ask_every_time"))
        assertEquals(ExifStripAction.SAVE_AS_COPY, ExifStripAction.fromId("save_as_copy"))
        assertEquals(ExifStripAction.OVERWRITE_ORIGINAL, ExifStripAction.fromId("overwrite_original"))
        assertEquals(ExifStripAction.ASK_EVERY_TIME, ExifStripAction.fromId("unknown_value"))
        assertEquals(ExifStripAction.ASK_EVERY_TIME, ExifStripAction.fromId(""))
    }
}

