package com.inferno.gallery.ui.people

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PersonHeadGlowHelperTest {

    @Test
    fun calculateHeadBounds_standardCenteredSquareImage_expandsHeadAndMapsCorrectly() {
        // 1000x1000 image inside 1000x1000 viewport => fitScale = 1.0, dx = 0, dy = 0
        // Normalized face at center: [0.4, 0.4, 0.6, 0.6] -> width = 200, height = 200, left = 400, top = 400
        val bounds = PersonHeadGlowHelper.calculateHeadBounds(
            bboxLeft = 0.4f,
            bboxTop = 0.4f,
            bboxRight = 0.6f,
            bboxBottom = 0.6f,
            imageWidth = 1000f,
            imageHeight = 1000f,
            viewportWidth = 1000f,
            viewportHeight = 1000f
        )

        assertNotNull(bounds)
        // faceW = 200, faceH = 200
        // padTop = 200 * 0.28 = 56 -> top = 400 - 56 = 344
        // padSide = 200 * 0.14 = 28 -> left = 400 - 28 = 372, right = 600 + 28 = 628
        // padBottom = 200 * 0.10 = 20 -> bottom = 600 + 20 = 620
        assertEquals(372f, bounds!!.left, 0.01f)
        assertEquals(344f, bounds.top, 0.01f)
        assertEquals(628f, bounds.right, 0.01f)
        assertEquals(620f, bounds.bottom, 0.01f)
    }

    @Test
    fun calculateHeadBounds_letterboxedPortrait_calculatesFitScaleAndDxCorrectly() {
        // Image is 1000w x 2000h (aspect 1:2) in a 1000w x 1000h viewport
        // fitScale = min(1000/1000, 1000/2000) = 0.5
        // rendered image width = 500, height = 1000
        // dx = (1000 - 500)/2 = 250, dy = 0
        val bounds = PersonHeadGlowHelper.calculateHeadBounds(
            bboxLeft = 0.2f,
            bboxTop = 0.1f,
            bboxRight = 0.4f,
            bboxBottom = 0.3f,
            imageWidth = 1000f,
            imageHeight = 2000f,
            viewportWidth = 1000f,
            viewportHeight = 1000f
        )

        assertNotNull(bounds)
        // faceX = 250 + (0.2 * 1000 * 0.5) = 250 + 100 = 350
        // faceY = 0 + (0.1 * 2000 * 0.5) = 100
        // faceW = 0.2 * 1000 * 0.5 = 100
        // faceH = 0.2 * 2000 * 0.5 = 200
        // padTop = 200 * 0.28 = 56 -> top = 100 - 56 = 44
        // padSide = 100 * 0.14 = 14 -> left = 350 - 14 = 336, right = 450 + 14 = 464
        // padBottom = 200 * 0.10 = 20 -> bottom = 300 + 20 = 320
        assertEquals(336f, bounds!!.left, 0.01f)
        assertEquals(44f, bounds.top, 0.01f)
        assertEquals(464f, bounds.right, 0.01f)
        assertEquals(320f, bounds.bottom, 0.01f)
    }

    @Test
    fun calculateHeadBounds_invalidDimensions_returnsNull() {
        assertNull(PersonHeadGlowHelper.calculateHeadBounds(0.1f, 0.1f, 0.5f, 0.5f, 0f, 1000f, 500f, 500f))
        assertNull(PersonHeadGlowHelper.calculateHeadBounds(0.5f, 0.5f, 0.1f, 0.1f, 1000f, 1000f, 500f, 500f))
        assertNull(PersonHeadGlowHelper.calculateHeadBounds(0.1f, 0.1f, 0.5f, 0.5f, Float.NaN, 1000f, 500f, 500f))
    }
}
