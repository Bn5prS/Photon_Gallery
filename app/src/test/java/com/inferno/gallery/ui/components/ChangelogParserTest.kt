package com.inferno.gallery.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogParserTest {

    @Test
    fun parseChangelog_emptyString_returnsEmptyList() {
        val result = parseChangelog("")
        assertTrue(result.isEmpty())

        val blankResult = parseChangelog("   \n\n  ")
        assertTrue(blankResult.isEmpty())
    }

    @Test
    fun parseChangelog_explicitSections_parsesCorrectly() {
        val raw = """
            ### Features
            - Add smart search with offline embeddings
            - Add video frame extractor
            
            ### Bug Fixes
            - Fix stark white canvas
            - Fix floating elements clutter in media viewer
        """.trimIndent()

        val sections = parseChangelog(raw)
        assertEquals(2, sections.size)

        assertEquals("Features", sections[0].title)
        assertEquals(2, sections[0].items.size)
        assertEquals("Add smart search with offline embeddings", sections[0].items[0])
        assertEquals("Add video frame extractor", sections[0].items[1])

        assertEquals("Bug Fixes", sections[1].title)
        assertEquals(2, sections[1].items.size)
        assertEquals("Fix stark white canvas", sections[1].items[0])
        assertEquals("Fix floating elements clutter in media viewer", sections[1].items[1])
    }

    @Test
    fun parseChangelog_githubAutoNotes_categorizesFeaturesAndFixes() {
        val raw = """
            ## What's Changed
            * Add gesture zoom for video player by @dev in #101
            * Fix crash on opening corrupted media by @dev in #102
            * Update dependencies and libraries by @dev in #103
            
            **Full Changelog**: https://github.com/Bn5prS/Photon_Gallery/compare/v1.0...v1.1
        """.trimIndent()

        val sections = parseChangelog(raw)
        assertTrue(sections.isNotEmpty())

        val featureSection = sections.find { it.title.contains("Features") }
        val fixSection = sections.find { it.title.contains("Fixes") }
        val otherSection = sections.find { it.title.contains("Other") }

        assertTrue(featureSection != null)
        assertEquals("Add gesture zoom for video player", featureSection!!.items[0])

        assertTrue(fixSection != null)
        assertEquals("Fix crash on opening corrupted media", fixSection!!.items[0])

        assertTrue(otherSection != null)
        assertEquals("Update dependencies and libraries", otherSection!!.items[0])

        // Verify Full Changelog link was ignored
        val allItems = sections.flatMap { it.items }
        assertTrue(allItems.none { it.contains("Full Changelog") || it.contains("compare") })
    }
}
