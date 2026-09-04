from pathlib import Path

p = Path('app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt')
s = p.read_text()

old_call = '''            PBottomNav(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            )'''
new_call = '''            PBottomNav(
                selected = tab,
                onSelect = { tab = it },
                onCreate = { openComposer() },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            )'''

old_fun = '''@Composable
private fun PBottomNav(selected: PTab, onSelect: (PTab) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier, RoundedCornerShape(24.dp), Color(0xF2161618), border = BorderStroke(1.dp, CinemaLine), shadowElevation = 12.dp) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 7.dp), horizontalArrangement = Arrangement.SpaceAround) {
            listOf(
                Triple(PTab.TODAY, Icons.Outlined.Home, "Today"),
                Triple(PTab.PLAN, Icons.Outlined.CalendarMonth, "Plan"),
                Triple(PTab.STUDIO, Icons.Outlined.MovieEdit, "Studio"),
                Triple(PTab.INSIGHTS, Icons.Outlined.Insights, "Insights"),
            ).forEach { (tab, icon, label) ->
                val active = tab == selected
                Surface(onClick = { onSelect(tab) }, shape = RoundedCornerShape(16.dp), color = if (active) Color(0xFF282326) else Color.Transparent, modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icon, label, tint = if (active) RecRed else MutedText, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.height(2.dp)); Text(label, color = if (active) ProjectorIvory else MutedText, fontSize = 7.8.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}'''

new_fun = '''@Composable
private fun PBottomNav(
    selected: PTab,
    onSelect: (PTab) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier,
        RoundedCornerShape(24.dp),
        Color(0xF2161618),
        border = BorderStroke(1.dp, CinemaLine),
        shadowElevation = 12.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            fun Modifier.navWeight() = this.weight(1f)

            @Composable
            fun NavItem(tab: PTab, icon: ImageVector, label: String) {
                val active = tab == selected
                Surface(
                    onClick = { onSelect(tab) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (active) Color(0xFF282326) else Color.Transparent,
                    modifier = Modifier.navWeight(),
                ) {
                    Column(
                        Modifier.padding(vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            icon,
                            label,
                            tint = if (active) RecRed else MutedText,
                            modifier = Modifier.size(21.dp),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            label,
                            color = if (active) ProjectorIvory else MutedText,
                            fontSize = 7.8.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }

            NavItem(PTab.TODAY, Icons.Outlined.Home, "Today")
            NavItem(PTab.PLAN, Icons.Outlined.CalendarMonth, "Plan")

            Box(Modifier.navWeight(), contentAlignment = Alignment.Center) {
                Surface(
                    onClick = onCreate,
                    modifier = Modifier.size(49.dp).offset(y = (-3).dp),
                    shape = CircleShape,
                    color = RecRed,
                    shadowElevation = 9.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Add,
                            "New project",
                            tint = ProjectorIvory,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }

            NavItem(PTab.STUDIO, Icons.Outlined.MovieEdit, "Studio")
            NavItem(PTab.INSIGHTS, Icons.Outlined.Insights, "Insights")
        }
    }
}'''

for label, old, new in [('call', old_call, new_call), ('function', old_fun, new_fun)]:
    count = s.count(old)
    if count != 1:
        raise SystemExit(f'Expected one {label} match, found {count}')
    s = s.replace(old, new, 1)

p.write_text(s)
print('v1.7.1 bottom navigation polish applied')
