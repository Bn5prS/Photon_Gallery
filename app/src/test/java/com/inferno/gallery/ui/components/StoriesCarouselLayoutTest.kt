package com.inferno.gallery.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoriesCarouselLayoutTest {

    @Test
    fun cardDimensionsScaleResponsivelyAndExceedSpotlightHeight() {
        val narrowWidth = computeStoryCardWidthDp(360f)
        val phoneWidth = computeStoryCardWidthDp(412f)
        val tabletWidth = computeStoryCardWidthDp(840f)

        assertTrue(narrowWidth in 148f..188f)
        assertTrue(phoneWidth in 160f..180f)
        assertEquals(188f, tabletWidth, 0.01f)

        // All responsive heights must exceed Spotlight's 196.dp height
        assertTrue(computeStoryCardHeightDp(narrowWidth) > 196f)
        assertTrue(computeStoryCardHeightDp(phoneWidth) > 196f)
        assertTrue(computeStoryCardHeightDp(tabletWidth) > 196f)
    }

    @Test
    fun indicatorIndexCoversFirstMiddleAndLastDotsSmoothly() {
        val total = 5
        val itemSize = 172
        val spacing = 10
        val pad = 16
        val viewport = 412
        // Total track = 16 + 5*172 + 4*10 + 16 = 932 -> maxScroll = 932 - 412 = 520

        // Unscrolled at start
        assertEquals(
            0,
            computeStoriesIndicatorIndex(
                totalItems = total,
                firstVisibleIndex = 0,
                scrolledFromStartPx = 0,
                itemSizePx = itemSize,
                itemSpacingPx = spacing,
                beforeContentPaddingPx = pad,
                afterContentPaddingPx = pad,
                viewportWidthPx = viewport,
                canScrollForward = true
            )
        )

        // Mid-scroll (260px of 520px = 50% -> index 2 of 0..4)
        assertEquals(
            2,
            computeStoriesIndicatorIndex(
                totalItems = total,
                firstVisibleIndex = 1,
                scrolledFromStartPx = 78, // 1 * 182 + 78 = 260
                itemSizePx = itemSize,
                itemSpacingPx = spacing,
                beforeContentPaddingPx = pad,
                afterContentPaddingPx = pad,
                viewportWidthPx = viewport,
                canScrollForward = true
            )
        )

        // Reached far right end (!canScrollForward) -> guarantees last dot lights up
        assertEquals(
            4,
            computeStoriesIndicatorIndex(
                totalItems = total,
                firstVisibleIndex = 2,
                scrolledFromStartPx = 156,
                itemSizePx = itemSize,
                itemSpacingPx = spacing,
                beforeContentPaddingPx = pad,
                afterContentPaddingPx = pad,
                viewportWidthPx = viewport,
                canScrollForward = false
            )
        )
    }

    @Test
    fun indicatorIndexHandlesSingleAndNonScrollableListsSafely() {
        assertEquals(
            0,
            computeStoriesIndicatorIndex(
                totalItems = 1,
                firstVisibleIndex = 0,
                scrolledFromStartPx = 0,
                itemSizePx = 172,
                itemSpacingPx = 10,
                beforeContentPaddingPx = 16,
                afterContentPaddingPx = 16,
                viewportWidthPx = 412,
                canScrollForward = false
            )
        )

        // Two items fitting inside a wide viewport with zero scroll
        assertEquals(
            0,
            computeStoriesIndicatorIndex(
                totalItems = 2,
                firstVisibleIndex = 0,
                scrolledFromStartPx = 0,
                itemSizePx = 172,
                itemSpacingPx = 10,
                beforeContentPaddingPx = 16,
                afterContentPaddingPx = 16,
                viewportWidthPx = 600,
                canScrollForward = false
            )
        )
    }
}
