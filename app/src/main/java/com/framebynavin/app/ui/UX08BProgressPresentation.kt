package com.framebynavin.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.ProgressJourneyEvent
import com.framebynavin.app.data.ProgressJourneyKind
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** UI projections only. A recorded event is NOT an hour worked or XP award. */
internal data class UX08BJourneySummary(
    val recordedEvents: Int,
    val recordedDays: Int,
    val creatorEvents: Int,
    val personalDoneReceipts: Int,
)

internal object UX08BProgressPolicy {
    fun summarize(events: List<ProgressJourneyEvent>, zone: ZoneId): UX08BJourneySummary =
        UX08BJourneySummary(
            recordedEvents = events.size,
            recordedDays = events.mapTo(HashSet()) { it.localDate(zone) }.size,
            creatorEvents = events.count { it.kind != ProgressJourneyKind.PERSONAL_COMPLETED },
            personalDoneReceipts = events.count { it.kind == ProgressJourneyKind.PERSONAL_COMPLETED },
        )

    fun forMonth(
        events: List<ProgressJourneyEvent>,
        month: YearMonth,
        zone: ZoneId,
    ): List<ProgressJourneyEvent> =
        events.filter { YearMonth.from(it.localDate(zone)) == month }

    /** Routes only to an original, still-present source. Private Done never
     * opens a Creator project; deleted Creator source has no dead button. */
    fun canOpen(
        event: ProgressJourneyEvent,
        projectIds: Set<String>,
        ideaIds: Set<String>,
    ): Boolean = when (event.kind) {
        ProgressJourneyKind.PERSONAL_COMPLETED -> false
        ProgressJourneyKind.IDEA_CAPTURED -> event.sourceId in ideaIds
        else -> event.sourceId in projectIds
    }

    fun sourceLabel(kind: ProgressJourneyKind): String = when (kind) {
        ProgressJourneyKind.PROJECT_CREATED -> "PROJECT CREATED"
        ProgressJourneyKind.STAGE_MOVED -> "PROJECT STAGE"
        ProgressJourneyKind.PROJECT_COMPLETED -> "PROJECT COMPLETED"
        ProgressJourneyKind.PROJECT_PUBLISHED -> "PUBLISH RECORDED"
        ProgressJourneyKind.IDEA_CAPTURED -> "IDEA SAVED"
        ProgressJourneyKind.PERSONAL_COMPLETED -> "PRIVATE PERSONAL DONE"
    }

    /**
     * In compact Week mode the arrows must move a WEEK, not a hidden month;
     * in Month mode they move a month as originally designed.
     */
    fun shiftPeriod(
        view: String,
        selected: LocalDate,
        month: YearMonth,
        direction: Long,
    ): Pair<LocalDate, YearMonth> {
        require(direction == -1L || direction == 1L)
        val day = if (view == "WEEK") selected.plusWeeks(direction)
            else month.plusMonths(direction).atDay(1)
        return day to YearMonth.from(day)
    }
}

@Composable
internal fun UX08BJourneySummaryStrip(
    summary: UX08BJourneySummary,
    partialCreatorHistory: Boolean,
    personalStatus: String?,
    includePersonal: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().testTag("ux08b-journey-summary"),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(
            "RECORDED HISTORY",
            color = MutedGold,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CinemaSurface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                UX08BMetric(summary.recordedEvents.toString(), "Events", Modifier.weight(1f))
                UX08BMetric(summary.recordedDays.toString(), "Days with records", Modifier.weight(1f))
                UX08BMetric(summary.creatorEvents.toString(), "Creator events", Modifier.weight(1f))
            }
        }
        if (partialCreatorHistory) {
            Text(
                "Partial Creator history: the workflow timeline could not be read. Saved projects and ideas are still shown; your timeline has not been reset.",
                color = MutedText,
                fontSize = 11.sp,
                modifier = Modifier.testTag("ux08b-partial-timeline"),
            )
        }
        if (includePersonal && personalStatus == null) {
            Text(
                "${summary.personalDoneReceipts} accepted private Personal Done receipt" +
                    (if (summary.personalDoneReceipts == 1) "" else "s"),
                color = MutedText,
                fontSize = 11.sp,
                modifier = Modifier.testTag("ux08b-personal-done-count"),
            )
        }
        if (personalStatus != null) {
            Text(
                personalStatus,
                color = MutedText,
                fontSize = 11.sp,
                modifier = Modifier.testTag("ux08b-personal-availability"),
            )
        }
        Text(
            "Events reflect saved actions, not hours worked or points earned.",
            color = MutedText,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun UX08BMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            value,
            color = ProjectorIvory,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(label, color = MutedText, fontSize = 10.sp, lineHeight = 13.sp)
    }
}
