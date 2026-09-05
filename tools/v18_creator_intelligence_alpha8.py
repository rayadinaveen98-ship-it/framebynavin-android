from pathlib import Path

ROOT = Path('.')
BUILD = ROOT / 'app/build.gradle.kts'
ENGINE = ROOT / 'app/src/main/java/com/framebynavin/app/youtube/YouTubeInsightEngine.kt'
PULSE = ROOT / 'app/src/main/java/com/framebynavin/app/youtube/YouTube24HourPulse.kt'
UI = ROOT / 'app/src/main/java/com/framebynavin/app/ui/V11YouTubeInsights.kt'
CLASSIFIER = ROOT / 'app/src/main/java/com/framebynavin/app/youtube/YouTubeContentClassifier.kt'
ENGINE_TEST = ROOT / 'app/src/test/java/com/framebynavin/app/youtube/YouTubeInsightEngineV172Test.kt'
GENERAL_TEST = ROOT / 'app/src/test/java/com/framebynavin/app/youtube/YouTubeCreatorGeneralizationAlpha8Test.kt'


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {count}')
    return text.replace(old, new, 1)


build = BUILD.read_text()
build = replace_once(build, 'versionCode = 47', 'versionCode = 48', 'versionCode')
build = replace_once(
    build,
    'versionName = "1.8.0-foundation-alpha7"',
    'versionName = "1.8.0-foundation-alpha8"',
    'versionName',
)
BUILD.write_text(build)

CLASSIFIER.write_text(r'''package com.framebynavin.app.youtube

import com.framebynavin.app.data.CreatorTask
import java.util.Locale

/**
 * Creator-neutral YouTube format classification.
 *
 * Structured project metadata wins whenever it is meaningful. Title inference is only a fallback
 * for generic project types such as "Video" so the analytics layer does not depend on one niche,
 * channel name, language, or creator-specific naming convention.
 */
object YouTubeContentClassifier {
    fun label(task: CreatorTask): String = label(task.title, task.contentType)

    fun label(title: String, contentType: String): String {
        val rawType = contentType.trim()
        val type = rawType.lowercase(Locale.ROOT)

        when {
            type.contains("short") || type.contains("reel") -> return "Short-form"
            type.contains("long-form") || type.contains("long form") -> return "Long-form"
            type.contains("live") || type.contains("stream") -> return "Live"
            type.contains("podcast") -> return "Podcast"
            type.contains("interview") -> return "Interview"
            rawType.isNotBlank() && type !in GENERIC_TYPES -> return rawType
        }

        val normalizedTitle = " " + title
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{M}\\p{N}#]+"), " ")
            .trim() + " "

        return when {
            " #shorts " in normalizedTitle || " short " in normalizedTitle -> "Short-form"
            " live " in normalizedTitle || " stream " in normalizedTitle -> "Live"
            " podcast " in normalizedTitle -> "Podcast"
            " interview " in normalizedTitle -> "Interview"
            " tutorial " in normalizedTitle || " how to " in normalizedTitle -> "Tutorial / How-to"
            " review " in normalizedTitle -> "Review"
            " breakdown " in normalizedTitle || " analysis " in normalizedTitle || " explained " in normalizedTitle -> "Analysis"
            " recommend " in normalizedTitle || " recommendation " in normalizedTitle -> "Recommendation"
            " vlog " in normalizedTitle -> "Vlog"
            rawType.isNotBlank() -> rawType
            else -> "Video"
        }
    }

    private val GENERIC_TYPES = setOf(
        "video",
        "youtube video",
        "youtube",
        "content",
        "production",
        "other",
    )
}
''')

engine = ENGINE.read_text()
engine = replace_once(
    engine,
    '            pillar(task) to video',
    '            YouTubeContentClassifier.label(task) to video',
    'format classifier call',
)
engine = engine.replace(
    '"FrameByNavin is learning your normal 24-hour pace. Refresh YouTube over time and this card will show what is rising, steady or slowing down."',
    '"Your creator system is learning your normal 24-hour pace. Refresh YouTube over time and this card will show what is rising, steady or slowing down."',
)
engine = engine.replace(
    '"FrameByNavin gets more useful as you connect published videos to the projects that made them."',
    '"Insights get more useful as you connect published videos to the projects that made them."',
)
old_pillar = '''    private fun pillar(task: CreatorTask): String {\n        val title = task.title.lowercase(Locale.getDefault())\n        val type = task.contentType.lowercase(Locale.getDefault())\n        return when {\n            title.contains("frame breakdown") -> "Frame Breakdown"\n            title.contains("why this scene works") -> "Why This Scene Works"\n            type.contains("cinematic moment") -> "Every Cinematic Moment"\n            type.contains("long-form") || type.contains("long form") -> "Long-form Analysis"\n            title.contains("review") || title.contains("recommend") -> "Reviews / Recommendations"\n            type.contains("short") || type.contains("reel") -> "Short-form"\n            else -> task.contentType.ifBlank { "Other" }\n        }\n    }\n\n'''
engine = replace_once(engine, old_pillar, '', 'founder-specific pillar classifier')
ENGINE.write_text(engine)

ui = UI.read_text()
old_ui_pillar = '''private fun ytPillar(task: CreatorTask): String {\n    val title = task.title.lowercase(Locale.getDefault())\n    val type = task.contentType.lowercase(Locale.getDefault())\n    return when {\n        title.contains("frame breakdown") -> "Frame Breakdown"\n        title.contains("why this scene works") -> "Why This Scene Works"\n        type.contains("cinematic moment") -> "Every Cinematic Moment"\n        type.contains("long-form") || type.contains("long form") -> "FrameByNavin Analysis"\n        title.contains("review") || title.contains("recommend") -> "Reviews / Recommendations"\n        type.contains("short") -> "YouTube Shorts"\n        else -> task.contentType\n    }\n}\n'''
ui = replace_once(
    ui,
    old_ui_pillar,
    'private fun ytPillar(task: CreatorTask): String = YouTubeContentClassifier.label(task)\n',
    'UI pillar classifier',
)
UI.write_text(ui)

pulse = PULSE.read_text()
pulse = pulse.replace('import com.framebynavin.app.data.IdeaVaultLabels\n', '')
pulse = replace_once(
    pulse,
    '''                val ideaText = listOf(\n                    idea.title,\n                    idea.topic,\n                    idea.notes,\n                    IdeaVaultLabels.category(idea.category),\n                ).joinToString(" ")''',
    '''                val ideaText = listOf(\n                    idea.title,\n                    idea.topic,\n                    idea.notes,\n                ).joinToString(" ")''',
    'idea match text',
)
pulse = replace_once(
    pulse,
    '.replace(Regex("[^a-z0-9]+"), " ")',
    '.replace(Regex("[^\\\\p{L}\\\\p{M}\\\\p{N}]+"), " ")',
    'Unicode tokenization',
)
old_stopwords = '''    private val STOP_WORDS = setOf(\n        "the", "and", "for", "with", "from", "this", "that", "movie", "film", "video", "review", "analysis",\n        "official", "trailer", "telugu", "cinema", "short", "shorts", "every", "why", "how", "best", "new",\n    )'''
new_stopwords = '''    private val STOP_WORDS = setOf(\n        "the", "and", "for", "with", "from", "this", "that", "these", "those", "your", "you", "our",\n        "are", "was", "were", "into", "about", "after", "before", "video", "videos", "review", "analysis",\n        "official", "short", "shorts", "every", "why", "how", "what", "when", "where", "who", "best", "new",\n    )'''
pulse = replace_once(pulse, old_stopwords, new_stopwords, 'creator-neutral stop words')
PULSE.write_text(pulse)

engine_test = ENGINE_TEST.read_text()
engine_test = replace_once(
    engine_test,
    'val long = rows.first { it.label == "Long-form Analysis" }',
    'val long = rows.first { it.label == "Long-form" }',
    'long-form expectation',
)
ENGINE_TEST.write_text(engine_test)

GENERAL_TEST.write_text(r'''package com.framebynavin.app.youtube

import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeCreatorGeneralizationAlpha8Test {
    @Test
    fun creatorDefinedFormatWinsWithoutNicheRules() {
        val task = CreatorTask(
            id = "gaming",
            title = "Rank push with the squad",
            platform = "YouTube",
            contentType = "Gameplay Challenge",
            dueLabel = "",
        )

        assertEquals("Gameplay Challenge", YouTubeContentClassifier.label(task))
    }

    @Test
    fun genericVideoCanInferCreatorNeutralFormatFromTitle() {
        assertEquals(
            "Tutorial / How-to",
            YouTubeContentClassifier.label("How to light a desk setup", "Video"),
        )
        assertEquals(
            "Podcast",
            YouTubeContentClassifier.label("Founder podcast episode 12", "Content"),
        )
    }

    @Test
    fun longAndShortProjectTypesNormalizeConsistently() {
        assertEquals("Long-form", YouTubeContentClassifier.label("Deep dive", "Long-form"))
        assertEquals("Short-form", YouTubeContentClassifier.label("Quick tip", "YouTube Short"))
    }

    @Test
    fun opportunityMatchingSupportsUnicodeCreatorTopics() {
        val report = YouTube24HourReport(
            sampleHours = 24,
            viewsGained = 600,
            subscribersDelta = 2,
            previousViewsGained = 300,
            viewsChangePercent = 100,
            momentum = YouTubePulseMomentum.RISING,
            topMovers = listOf(
                YouTubePulseMover(
                    videoId = "unicode-1",
                    title = "కెమెరా లైటింగ్ టిప్స్",
                    viewsGained = 300,
                    channelGainSharePercent = 50,
                )
            ),
            currentCapturedAtMillis = 2L,
            baselineCapturedAtMillis = 1L,
        )
        val ideas = listOf(
            CreatorIdea(
                id = "unicode-match",
                title = "కెమెరా లైటింగ్ ఐడియా",
                topic = "లైటింగ్",
            )
        )

        val alerts = YouTubeOpportunityEngine.build(report, ideas)

        assertTrue(alerts.any { it.kicker == "MATCHED IDEA" && it.ideaId == "unicode-match" })
    }
}
''')

print('Applied v1.8 Foundation Alpha8 creator-neutral YouTube intelligence')
