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

    @Test
    fun parseChangelog_emojisInHeadersAndItems_stripsEmojisAndResolvesM3Shapes() {
        val raw = """
            # Release Notes
            
            ## What's New
            
            ### 👥 People & Face Grouping (New Feature!)
            Photon Gallery can now automatically group faces 🔒
            * **People Section**: Dedicated tab with clean portrait cards
            
            ---
            
            ### 🎨 Markup & Paint Tools in Editor (#20)
            * **Drawing & Annotation**: Complete markup toolset inside editor
            
            ---
            
            ### 🔔 Instant Update Notifications
            * **Prompt on Launch**: Checks on launch with cooldown ⚡
            
            ---
            
            ### 🐛 Bug Fixes & Stability
            * **Direct Share (#17)**: Fixed attachments
        """.trimIndent()

        val sections = parseChangelog(raw)
        assertEquals(4, sections.size)

        // 1. People section
        assertEquals("People & Face Grouping (New Feature!)", sections[0].title)
        assertEquals(com.inferno.gallery.R.drawable.ic_ms_person, sections[0].iconRes)
        assertEquals(2, sections[0].items.size)
        assertEquals("Photon Gallery can now automatically group faces", sections[0].items[0])
        assertEquals("People Section: Dedicated tab with clean portrait cards", sections[0].items[1])

        // 2. Editor section
        assertEquals("Markup & Paint Tools in Editor (#20)", sections[1].title)
        assertEquals(com.inferno.gallery.R.drawable.ic_ms_edit, sections[1].iconRes)
        assertEquals(1, sections[1].items.size)
        assertEquals("Drawing & Annotation: Complete markup toolset inside editor", sections[1].items[0])

        // 3. Notifications section
        assertEquals("Instant Update Notifications", sections[2].title)
        assertEquals(com.inferno.gallery.R.drawable.ic_ms_schedule, sections[2].iconRes)
        assertEquals(1, sections[2].items.size)
        assertEquals("Prompt on Launch: Checks on launch with cooldown", sections[2].items[0])

        // 4. Bug fixes section
        assertEquals("Bug Fixes & Stability", sections[3].title)
        assertEquals(com.inferno.gallery.R.drawable.ic_ms_bug_report, sections[3].iconRes)
        assertEquals(1, sections[3].items.size)
        assertEquals("Direct Share (#17): Fixed attachments", sections[3].items[0])

        // Verify dividers ("---") were never included as items
        val allItems = sections.flatMap { it.items }
        assertTrue(allItems.none { it.contains("---") || it == "--" })
    }

    @Test
    fun stripEmojis_variousSymbols_removesThemCleanly() {
        assertEquals("People & Face Grouping", stripEmojis("👥 People & Face Grouping"))
        assertEquals("Markup & Paint Tools", stripEmojis("🎨 Markup & Paint Tools"))
        assertEquals("Instant Update Notifications", stripEmojis("🔔 Instant Update Notifications"))
        assertEquals("Bug Fixes & Stability", stripEmojis("🐛 Bug Fixes & Stability"))
        assertEquals("100% locally with privacy", stripEmojis("100% locally with privacy 🔒"))
    }
}

