from pathlib import Path

ui = Path('app/src/main/java/com/framebynavin/app/ui/V11YouTubeInsights.kt')
s = ui.read_text()

old_intro = '''            Text("What is actually working?", color = ProjectorIvory, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("Your creator workflow and real YouTube performance in one place.", color = MutedText, fontSize = 10.5.sp)'''
new_intro = '''            Text("What matters — and what next?", color = ProjectorIvory, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("Performance, causes and creator decisions in one place.", color = MutedText, fontSize = 10.5.sp)'''
if s.count(old_intro) != 1:
    raise SystemExit(f'Expected one insights intro, found {s.count(old_intro)}')
s = s.replace(old_intro, new_intro, 1)

old_stack = '''                Spacer(Modifier.height(14.dp))
                YTMetrics(data)
                Spacer(Modifier.height(18.dp))
                YTSignalCard(data)
                Spacer(Modifier.height(18.dp))
                YTTrendCard(data)
                Spacer(Modifier.height(18.dp))
                YTTopVideos(data, tasks, links) { selectedVideo = it }
                Spacer(Modifier.height(18.dp))
                YTFormatSignal(data, tasks, links)
                Spacer(Modifier.height(18.dp))
                V16CreatorIntelligenceCard(tasks, ideas, data, links)
                Spacer(Modifier.height(18.dp))
                YTRecentVideos(data, tasks, links) { selectedVideo = it }
                Spacer(Modifier.height(18.dp))
                YTLocalCreatorSection(tasks, ideas)'''
new_stack = '''                Spacer(Modifier.height(14.dp))
                V172InsightsBody(
                    snapshot = data,
                    tasks = tasks,
                    ideas = ideas,
                    links = links,
                    onLinkVideo = { selectedVideo = it },
                )'''
if s.count(old_stack) != 1:
    raise SystemExit(f'Expected one legacy insights stack, found {s.count(old_stack)}')
s = s.replace(old_stack, new_stack, 1)
ui.write_text(s)

engine = Path('app/src/main/java/com/framebynavin/app/youtube/YouTubeInsightEngine.kt')
e = engine.read_text()
old_fmt = 'String.format(Locale.US, "%.1K h", hours / 1000.0)'
new_fmt = 'String.format(Locale.US, "%.1fK h", hours / 1000.0)'
if e.count(old_fmt) != 1:
    raise SystemExit(f'Expected one watch format typo, found {e.count(old_fmt)}')
engine.write_text(e.replace(old_fmt, new_fmt, 1))

print('v1.7.2 Insights 2.0 live wiring applied')
