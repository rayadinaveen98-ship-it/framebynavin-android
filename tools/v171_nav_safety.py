from pathlib import Path

p = Path('app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt')
s = p.read_text()
start = s.index('@Composable\nprivate fun PBottomNav(')
end = s.index('\n@Composable\nprivate fun PHomeGreetingHeader', start)
old = s[start:end]
new = '''@Composable
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
            PBottomNavItem(
                tab = PTab.TODAY,
                icon = Icons.Outlined.Home,
                label = "Today",
                active = selected == PTab.TODAY,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
            PBottomNavItem(
                tab = PTab.PLAN,
                icon = Icons.Outlined.CalendarMonth,
                label = "Plan",
                active = selected == PTab.PLAN,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )

            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
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

            PBottomNavItem(
                tab = PTab.STUDIO,
                icon = Icons.Outlined.MovieEdit,
                label = "Studio",
                active = selected == PTab.STUDIO,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
            PBottomNavItem(
                tab = PTab.INSIGHTS,
                icon = Icons.Outlined.Insights,
                label = "Insights",
                active = selected == PTab.INSIGHTS,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PBottomNavItem(
    tab: PTab,
    icon: ImageVector,
    label: String,
    active: Boolean,
    onSelect: (PTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { onSelect(tab) },
        shape = RoundedCornerShape(16.dp),
        color = if (active) Color(0xFF282326) else Color.Transparent,
        modifier = modifier,
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
'''
s = s[:start] + new + s[end:]
p.write_text(s)
print('v1.7.1 nav compile-safety patch applied')
