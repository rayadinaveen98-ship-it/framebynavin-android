from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_required(rel_path: str, replacements: list[tuple[str, str]]) -> None:
    path = ROOT / rel_path
    text = path.read_text(encoding="utf-8")
    for old, new in replacements:
        if old not in text:
            raise RuntimeError(f"Expected copy not found in {rel_path}: {old[:90]!r}")
        text = text.replace(old, new)
    path.write_text(text, encoding="utf-8")


# Keep this release on the v1.7.5 product line; only copy/presentation changes.
replace_required(
    "app/build.gradle.kts",
    [
        ("versionCode = 37", "versionCode = 38"),
        ("versionName = \"1.7.5-original-frames-splash-rc1\"", "versionName = \"1.7.5-ux-copy-polish-rc2\""),
    ],
)

replace_required(
    "app/src/main/java/com/framebynavin/app/ui/V172InsightsUi.kt",
    [
        ("PERFORMANCE PULSE", "CHANNEL PULSE"),
        ("CONTENT DRIVERS", "TOP VIDEOS"),
        ("The uploads doing the most work in this window.", "The videos driving your channel right now."),
        ("Sync after YouTube has enough report data to rank videos.", "Refresh YouTube to see which videos are performing best."),
        ("builds after sync", "waiting for comparison"),
        ("MOMENTUM", "DAILY VIEWS"),
        ("Daily views · last ${points.size} available days", "Last ${points.size} days"),
        ("RANKED PERFORMANCE", "VIDEO PERFORMANCE"),
        ("Ranked against the videos visible in this ${snapshot.windowDays}-day window.", "See which videos performed best in this period."),
        ("No video-level report data yet.", "No video performance data yet."),
        ("FORMAT / PILLAR PERFORMANCE", "WHAT WORKS BEST"),
        ("Normalized per linked upload — not just total views.", "Compare your content types fairly."),
        ("Link published YouTube videos to their Creator OS projects to unlock fair format comparisons.", "Connect published videos to projects to compare what works best."),
        ("${format.uploadCount} linked", "${format.uploadCount} connected"),
        ("VIEWS / UPLOAD", "AVG. VIEWS"),
        ("WATCH / UPLOAD", "AVG. WATCH TIME"),
        ("SUBS / 1K", "SUBSCRIBERS"),
        ("ENGAGE / 1K", "ENGAGEMENT"),
        ("CREATOR OPERATING SYSTEM", "YOUR CREATOR PROGRESS"),
        ("Platform performance beside the work required to produce it.", "See how your work and channel results connect."),
        ("30D PUBLISHED", "PUBLISHED THIS MONTH"),
        ("STARTED → DONE", "FINISHED"),
        ("VIDEOS LINKED", "VIDEOS CONNECTED"),
        ("WORKFLOW SIGNAL", "WORKFLOW"),
        ("No major workflow pile-up right now.", "Your workflow looks clear right now."),
        ("Finishing that queue may create more publishing momentum than starting another project.", "Finish these projects before starting too many new ones."),
        ("Keep linking published videos so production effort can be compared with actual performance.", "Keep connecting published videos so FrameByNavin can learn what works."),
        ("EFFORT → RETURN BRIDGE", "WHAT PAYS OFF"),
        ("Which linked content lane gives the strongest return per upload?", "Which type of content gives you the best results?"),
        ("Link published videos to projects to connect production choices with performance.", "Connect published videos to projects to see what works best."),
        ("CREATOR PROJECT", "CONNECTED PROJECT"),
        ("Not linked yet", "Not connected yet"),
        ("LINK PROJECT", "CONNECT PROJECT"),
        ("CHANGE LINK", "CHANGE PROJECT"),
        (
            'val baseline = if (performance.baselineMultiple > 0) "${String.format(Locale.US, "%.1f×", performance.baselineMultiple)} visible baseline" else "Building baseline"',
            'val baseline = if (performance.baselineMultiple > 0) {\n                    val difference = ((performance.baselineMultiple - 1.0) * 100).toInt()\n                    if (difference >= 0) "$difference% above your usual" else "${abs(difference)}% below your usual"\n                } else "Learning your usual performance"',
        ),
        ("$baseline · ${performance.viewSharePercent}% of channel window", "$baseline · ${performance.viewSharePercent}% of views this period"),
        (
            'val baseline = if (performance.baselineMultiple > 0) "${String.format(Locale.US, "%.1f×", performance.baselineMultiple)} visible-video baseline" else "Building baseline"',
            'val baseline = if (performance.baselineMultiple > 0) {\n                    val difference = ((performance.baselineMultiple - 1.0) * 100).toInt()\n                    if (difference >= 0) "$difference% above your usual" else "${abs(difference)}% below your usual"\n                } else "Learning your usual performance"',
        ),
        ("$baseline · ${performance.viewSharePercent}% of this ${windowDays}D window", "$baseline · ${performance.viewSharePercent}% of views in this period"),
        ("NET SUBS", "SUBSCRIBERS"),
    ],
)

replace_required(
    "app/src/main/java/com/framebynavin/app/youtube/YouTubeInsightEngine.kt",
    [
        ("Performance baseline ready", "Your usual range is ready"),
        ("Strong ${snapshot.windowDays}-day window", "Strong ${snapshot.windowDays}-day performance"),
        ("Cooling ${snapshot.windowDays}-day window", "Slower ${snapshot.windowDays}-day performance"),
        ("Growth with mixed signals", "Growing, but not everywhere"),
        ("A softer window — inspect the cause", "A slower period — check what changed"),
        ("Sync this window once to compare it with the immediately preceding ${snapshot.windowDays} days.", "Refresh again later to see how this period compares with the one before it."),
        ("Reach and viewing depth both improved versus the previous period.", "More people watched, and they stayed longer."),
        ("Reach improved, but viewers are leaving earlier than in the previous period.", "More people watched, but they left sooner."),
        ("Fewer people arrived, but the viewers who did stayed longer.", "Fewer people watched, but those who did stayed longer."),
        ("Both reach and viewing depth weakened versus the previous period.", "Fewer people watched, and they spent less time watching."),
        ("The biggest changes are small; focus on individual content performance before changing strategy.", "Things are fairly steady. Check individual videos before changing your approach."),
        ("Building your rolling baseline", "Learning your normal 24-hour pace"),
        ("FrameByNavin is now saving lightweight YouTube counter snapshots. Once two samples are roughly a day apart, this card will show rolling views, subscriber movement, momentum and top movers without pretending YouTube exposes hourly Analytics data.", "FrameByNavin is learning your normal 24-hour pace. Refresh YouTube over time and this card will show what is rising, steady or slowing down."),
        ("% vs prior window", "% vs before"),
        (" · top mover: ", " · top video: "),
        ("Measured across ~${pulse24h.sampleHours} hours of FrameByNavin counter history$changeText$topMover.", "Based on your last ~${pulse24h.sampleHours} hours of channel activity$changeText$topMover."),
        ("Open Idea Vault from Control to develop this saved idea.", "Open Idea Vault and build this idea."),
        ("DOUBLE DOWN", "WORKING WELL"),
        ("This video is running ${String.format(Locale.US, \"%.1f×\", top.baselineMultiple)} above the visible-video baseline and contributes ${top.viewSharePercent}% of this window's views.", "This video is doing much better than your recent-video average and is driving ${top.viewSharePercent}% of views in this period."),
        ("PERFORMANCE DRIVER", "TOP VIDEO"),
        ("One upload is responsible for ${top.viewSharePercent}% of this window's channel views. Protect what worked before changing format.", "This video is driving ${top.viewSharePercent}% of your views in this period. Look at what worked before changing direction."),
        ("Viewing depth fell ${abs(avgChange)}%", "Average view time fell ${abs(avgChange)}%"),
        ("Your average view duration is lower than the preceding ${snapshot.windowDays}-day period. Check intros and pacing before chasing more reach.", "People are leaving sooner than before. Check your opening and pacing."),
        ("QUALITY SIGNAL", "VIEWERS STAYED LONGER"),
        ("Viewing depth improved ${avgChange}%", "Average view time improved ${avgChange}%"),
        ("People are staying longer than in the preceding period. Study the openings and pacing of your strongest uploads.", "People are staying longer. Check what your strongest videos did well."),
        ("NEXT EXPERIMENT", "WHAT'S WORKING"),
        ("Averages ${compact(bestFormat.viewsPerUpload)} views and ${watch(bestFormat.watchMinutesPerUpload)} watch time per linked upload. Consider another project in this lane before spreading wider.", "On average, this gets ${compact(bestFormat.viewsPerUpload)} views and ${watch(bestFormat.watchMinutesPerUpload)} watch time per connected video. Consider another project like this."),
        ("Your current creator workload is bunching up around ${creator.bottleneckLabel ?: \"production\"}. Clearing that queue may unlock more publishing than starting something new.", "You have several active projects around ${creator.bottleneckLabel ?: \"production\"}. Finishing those may help more than starting something new."),
        ("BUILD THE BASELINE", "KEEP LEARNING"),
        ("Keep syncing and linking projects", "Keep refreshing and connecting projects"),
        ("FrameByNavin will become more specific as more videos are linked back to the projects that produced them.", "FrameByNavin gets more useful as you connect published videos to the projects that made them."),
    ],
)

replace_required(
    "app/src/main/java/com/framebynavin/app/youtube/YouTube24HourPulse.kt",
    [
        ("Your channel is accelerating", "Your channel is picking up"),
        ("${compact(report.viewsGained)} views arrived across the last ~${report.sampleHours} hours, ${signedPercent(report.viewsChangePercent)} versus the preceding comparable window. Look for a follow-up while the signal is fresh.", "You gained ${compact(report.viewsGained)} views in about ${report.sampleHours} hours, ${signedPercent(report.viewsChangePercent)} compared with before. This may be a good time for a follow-up."),
        ("CONTENT MOVING", "VIDEO PICKING UP"),
        ("This upload gained ${compact(top.viewsGained)} tracked views and accounts for about ${top.channelGainSharePercent}% of the channel's 24H gain. A related Short, follow-up or deeper angle is worth considering.", "This video gained ${compact(top.viewsGained)} views and drove about ${top.channelGainSharePercent}% of today's growth. A related Short or follow-up could work."),
        ("IDEA VAULT MATCH", "MATCHED IDEA"),
        ("A saved idea overlaps with the topic currently moving on your channel. Re-open it now instead of starting from zero.", "You already saved an idea related to this topic. Open it and build from there."),
        ("SUBSCRIBER SIGNAL", "NEW SUBSCRIBERS"),
        ("+${report.subscribersDelta} subscribers in the 24H pulse", "+${report.subscribersDelta} subscribers recently"),
        ("The same window that produced ${compact(report.viewsGained)} views also moved subscriber count upward. Check the top movers before choosing the next topic.", "Your recent views also brought in subscribers. Check your top videos before choosing what to make next."),
    ],
)

replace_required(
    "app/src/main/java/com/framebynavin/app/ui/V16CreatorIntelligenceUi.kt",
    [
        ("CREATOR INTELLIGENCE", "WHAT I'M LEARNING"),
        ("What FrameByNavin is learning", "Patterns from your work"),
        ("7D DONE", "DONE THIS WEEK"),
        ("30D DONE", "DONE THIS MONTH"),
        ("IDEA → PROJECT", "IDEAS STARTED"),
        ("Current bottleneck · $it (${review.bottleneckCount} active)", "Most work is waiting at · $it (${review.bottleneckCount})"),
        ("No workflow bottleneck yet.", "Your workflow looks clear."),
        ("Sync more YouTube data to build performance memory.", "Keep refreshing YouTube so I can learn what usually works."),
        ("${top.title} is running ${String.format(Locale.US, \"%.1f×\", multiple)} above the current visible-video average for this window.", "${top.title} is doing better than your recent-video average in this period."),
        ("${top.title} is the strongest visible video in this window at ${v16Compact(top.periodViews)} views.", "${top.title} is your strongest video in this period with ${v16Compact(top.periodViews)} views."),
        ("YouTube is connected. Keep syncing to build stronger comparisons.", "YouTube is connected. Keep refreshing so comparisons get better."),
        ("Link published videos to projects to unlock project-level memory.", "Connect published videos to projects so FrameByNavin can learn from them."),
        ("linked to FrameByNavin projects.", "connected to FrameByNavin projects."),
        ("checkpoint saved locally.", "24-hour result saved."),
    ],
)

replace_required(
    "app/src/main/java/com/framebynavin/app/ui/V17AutomationCenter.kt",
    [
        ("Creator Automation", "Automation"),
        ("Let the repetitive parts run.", "Let FrameByNavin handle repeat work."),
        ("FrameByNavin can prepare and remind. Publishing and destructive actions still stay with you.", "FrameByNavin can plan and remind. You still control publishing and deletion."),
        ("BACKGROUND AUTO PLAN", "AUTO PLAN"),
        ("14-day creator planner", "Plan the next 14 days"),
        ("schedule slots · $generated upcoming projects prepared", "weekly plans · $generated upcoming projects ready"),
        ("Off · your weekly schedule remains saved", "Off · your weekly plan stays saved"),
        ("Planner refresh requested", "Updating your plan…"),
        ("Last background check · ${v17Time(lastPlannerAt)} · $lastCreated added", "Last updated · ${v17Time(lastPlannerAt)} · $lastCreated added"),
        ("Background planner is scheduled automatically", "Your plan updates automatically"),
        ("RUN NOW", "UPDATE PLAN"),
        ("ALWAYS-AWARE AUTOMATION", "AUTOMATIC HELP"),
        ("Post-publish follow-ups", "After-publish reminders"),
        ("$postPublish follow-up actions currently active", "$postPublish after-publish reminders active"),
        ("Creates promotion + 24h + 7d follow-ups after YouTube publishing", "Adds promotion, 24-hour and 7-day reminders after you publish"),
        ("Context nudges", "Helpful reminders"),
        ("On · creator-risk checks remain separate from exact reminders", "On · warns you when a project may need attention"),
        ("Off · enable from Settings if you want at-risk creator nudges", "Off · turn this on in Settings for extra project reminders"),
        ("CREATOR ROUTINES", "REGULAR CHECK-INS"),
        ("Optional background notifications. They never create exact alarms.", "Choose the regular reminders you want from FrameByNavin."),
        ("You remain the final control", "You stay in control"),
        ("Automation can prepare projects, follow-ups and routine notifications. It does not publish, delete creator work or post to social accounts.", "FrameByNavin can plan and remind. It will never publish or delete anything without you."),
    ],
)

replace_required(
    "app/src/main/java/com/framebynavin/app/ui/V15ContextUi.kt",
    [
        ("Context from deadlines, progress and workflow stage.", "Things that may need your attention today."),
        ("${brief.reminderCount} reminders in 24h", "${brief.reminderCount} reminders today"),
        ("Projects and enabled weekly slots in one timeline. Weekly items already created as projects are shown only once.", "See your projects and weekly plan together."),
        ("Add project deadlines or enable Weekly Plan slots.", "Add a project date or turn on a weekly plan."),
    ],
)

replace_required(
    "app/src/main/java/com/framebynavin/app/ui/V09IdeaVaultUi.kt",
    [
        ("OPPORTUNITY MATCH", "MATCHED IDEA"),
        ("A topic moving in your 24H Pulse overlaps with this saved idea. It has been moved to the top of the vault.", "This idea matches a topic gaining attention on your channel, so I moved it to the top."),
        ("All pillars", "All topics"),
        ("TREND MATCH", "MATCHED"),
        ("CONTENT PILLAR", "TOPIC"),
        ("PROJECT", "TURN INTO PROJECT"),
    ],
)

replace_required(
    "app/src/main/java/com/framebynavin/app/ui/V131PolishUi.kt",
    [
        ("Completed projects now have archive and delete controls.", "Open a project to see what’s done and what comes next."),
        ("Studio is empty", "No projects yet"),
        ("Create a project and its production stages will live here.", "Create a project and its steps will appear here."),
        ("This permanently removes the project from Creator OS. If it came from Weekly Plan, this occurrence will stay suppressed instead of being regenerated.", "This permanently deletes the project. If it came from your Weekly Plan, this one will not be added again."),
    ],
)

replace_required(
    "app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt",
    [
        ("Your creator queue is clear.", "Your project list is clear."),
        ("COMPLETE CURRENT STAGE", "MARK STEP DONE"),
        ("Automation Center", "Automation"),
        ("Background planning, routines and automatic follow-ups", "Auto planning and regular reminders"),
        ("CONTEXT NUDGES", "HELPFUL REMINDERS"),
        ("Optional gentle alerts when active creator work is at risk.", "Extra reminders when a project may need attention."),
        ("Creator context nudges", "Helpful project reminders"),
        ("On · checks periodically for overdue or at-risk active work.", "On · warns you when active work may need attention."),
        ("Off · exact reminders still work normally.", "Off · your normal reminders still work."),
        ("SYNC & BACKUP", "BACKUP & SYNC"),
        ("Optional Google account protection. Local data remains primary.", "Keep a cloud copy while your phone remains the main copy."),
        ("Google account · restore points · local-first", "Google account · cloud backups"),
        ("YOUTUBE", "YOUTUBE CONNECTION"),
        ("Real channel performance, cached locally after each sync.", "See your real channel performance inside Insights."),
        ("Connect, sync and link published videos from Insights", "Connect YouTube and match published videos to projects"),
        ("DATA & BACKUP", "LOCAL BACKUP"),
        ("Export or restore your local Creator OS data.", "Save or restore a copy of your app data."),
    ],
)

replace_required(
    "app/src/main/java/com/framebynavin/app/ui/V11YouTubeInsights.kt",
    [
        ("Performance, causes and creator decisions in one place.", "See what changed, what worked and what to do next."),
        ("Read-only channel + analytics access", "See your channel performance"),
        ("Bring views, watch time, subscribers and video performance into Creator OS. FrameByNavin never asks for upload or delete permission.", "Bring views, watch time, subscribers and video performance into FrameByNavin. It cannot upload or delete your videos."),
        ("YOUTUBE SETUP", "YOUTUBE CONNECTION"),
        ("Text(\"Package · $packageName\", color = MutedText, fontSize = 8.2.sp)\n            Text(\"SHA-1 · $sha1\", color = MutedText, fontSize = 8.2.sp)", "Text(\"Try connecting again. If it still fails, the app setup may need attention.\", color = MutedText, fontSize = 8.2.sp)"),
        ("SYNC", "REFRESH"),
        ("synced ${ytSyncTime(data.fetchedAtMillis)}", "updated ${ytSyncTime(data.fetchedAtMillis)}"),
        ("Google OAuth is not configured for this app signature yet. Enable YouTube Data API + YouTube Analytics API and add this Android package/SHA-1 in Google Cloud.", "YouTube sign-in is not fully set up for this app yet."),
        ("The required YouTube APIs are not enabled for the Google Cloud project yet.", "YouTube connection is not fully enabled yet."),
        ("YouTube authorization expired. Connect again and retry.", "Your YouTube connection expired. Connect again."),
        ("YouTube sync failed. Check internet access and Google authorization.", "YouTube refresh failed. Check your internet and try connecting again."),
        ("Tap a video to link it to the Creator OS project that produced it.", "Tap a video to connect it to the project that made it."),
        ("Linked · ${it.title}", "Connected · ${it.title}"),
        ("Link project", "Connect project"),
        ("CONTENT SIGNAL", "WHAT'S WORKING"),
        ("Built from YouTube videos you link back to Creator OS projects.", "Based on published videos you connect to projects."),
        ("Link a few published videos to unlock format and pillar performance here.", "Connect a few published videos to see which content types work best."),
        ("Your local production momentum still matters beside platform numbers.", "Your project progress matters alongside your channel numbers."),
        ("No video-level analytics returned yet.", "No video performance data yet."),
    ],
)

replace_required(
    "app/src/main/java/com/framebynavin/app/cloud/CloudSyncActivity.kt",
    [
        ("Sync & Backup", "Backup & Sync"),
        ("Your phone stays primary.", "Your phone stays the main copy."),
        ("FrameByNavin keeps working offline. Cloud Sync protects Creator OS data and makes a future phone restore possible.", "FrameByNavin keeps working offline. Cloud backup keeps a copy of your projects, ideas and reminders."),
        ("Google identity only · no Gmail inbox access", "Google is only used to sign you in."),
        ("One-time Google Web OAuth client setup is still required before account sign-in can be enabled.", "Google sign-in isn’t ready yet."),
        ("Automatically protect your Creator OS data", "Automatically keep a cloud backup"),
        ("LAST SUCCESSFUL SYNC", "LAST BACKUP"),
        ("SYNC NOW", "BACK UP NOW"),
        ("RESTORE POINTS", "BACKUPS"),
        ("No cloud restore point yet. Tap Sync Now to create the first one.", "No cloud backup yet. Create one when you’re ready."),
        ("This replaces Creator OS data on this phone with the ${cloudPointLabel(point)} restore point. A rollback copy is created locally first.", "This replaces the app data on this phone with the selected backup. I’ll save a copy of your current data first."),
        ("Cloud backups and device records will be removed. Nothing stored locally on this phone will be deleted.", "Your cloud backups will be removed. Nothing stored on this phone will be deleted."),
        ("Safety snapshot", "Safety backup"),
        ("Cloud Sync turns off; local data stays", "Cloud backup turns off; phone data stays"),
    ],
)

print("v1.7.5 UX copy polish applied")
