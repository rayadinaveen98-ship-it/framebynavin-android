package com.pianostudio.alpha

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class P0SourceContractTest {
    private val main = File("src/main")
    @Test fun onlyFiveProductionActivitiesAreDeclared() {
        val manifest = File(main, "AndroidManifest.xml").readText()
        val names = Regex("<activity\\s+android:name=\"([^\"]+)\"").findAll(manifest).map { it.groupValues[1] }.toList()
        assertEquals(setOf(".StudioHomeActivity", ".LearningHubActivity", ".R2FreePianoActivity", ".R2PracticeActivity", ".R2LessonActivity"), names.toSet())
        assertEquals(5, names.size)
        assertEquals(1, Regex("android.intent.action.MAIN").findAll(manifest).count())
    }
    @Test fun obsoleteMarkerAndLegacySourcesAreAbsent() {
        val source = File(main, "java/com/pianostudio/alpha")
        listOf("MainActivity.kt", "PracticeCoachActivity.kt", "LandscapeLessonActivity.kt", "LearningActivity.kt", "R2Smoke.kt", "R2Noop.kt", "R2MarkerFinal.kt", "R2BuildTag.kt", "R2BuildProbe.txt", "R2TemporaryMarker.txt").forEach {
            assertFalse("Obsolete source must not be packaged: $it", File(source, it).exists())
        }
        assertTrue(File(source, "DesignCompat.kt").readText().contains("PianoStudioTheme"))
        assertTrue(File(source, "PracticeData.kt").readText().contains("typealias PianoSessionStore"))
    }
}
