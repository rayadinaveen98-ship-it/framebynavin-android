from pathlib import Path

p = Path('app/src/main/java/com/framebynavin/app/ui/V131PolishUi.kt')
s = p.read_text()
start_marker = '@Composable\ninternal fun V131HomeHeroSlideshow() {'
end_marker = '@Composable\ninternal fun V131PlanScreen('
start = s.find(start_marker)
end = s.find(end_marker, start)
if start < 0 or end < 0:
    raise SystemExit('Hero slideshow markers not found; refusing unsafe patch')

replacement = '''@Composable
internal fun V131HomeHeroSlideshow() {
    val resourceIds = remember {
        listOf(
            R.drawable.hero_frame_01,
            R.drawable.hero_frame_02,
            R.drawable.hero_frame_03,
            R.drawable.hero_frame_04,
            R.drawable.hero_frame_05,
            R.drawable.hero_frame_06,
            R.drawable.hero_frame_07,
            R.drawable.hero_frame_08,
            R.drawable.hero_frame_09,
            R.drawable.hero_frame_10,
        )
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(resourceIds.size) {
        while (resourceIds.size > 1) {
            delay(5_000L)
            index = (index + 1) % resourceIds.size
        }
    }
    val quoteIndex = index % heroQuotes.size

    Surface(
        modifier = Modifier.fillMaxWidth().height(248.dp),
        shape = RoundedCornerShape(22.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine.copy(alpha = .45f)),
        shadowElevation = 10.dp,
    ) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp))) {
            AnimatedContent(
                targetState = index.coerceIn(0, resourceIds.lastIndex),
                transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(700)) },
                label = "cinemaHeroNetflix",
            ) { visibleIndex ->
                Image(
                    painter = painterResource(resourceIds[visibleIndex]),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            // Netflix-style readable image treatment: image stays untouched; gradients are UI-only.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        listOf(
                            CinemaBlack.copy(alpha = .82f),
                            CinemaBlack.copy(alpha = .40f),
                            Color.Transparent,
                        )
                    )
                )
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            CinemaBlack.copy(alpha = .08f),
                            Color.Transparent,
                            CinemaBlack.copy(alpha = .18f),
                            CinemaBlack.copy(alpha = .88f),
                        )
                    )
                )
            )

            Row(
                Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(3.dp).height(14.dp).background(RecRed, RoundedCornerShape(4.dp)))
                Spacer(Modifier.width(7.dp))
                Text(
                    "FRAMEBYNAVIN  •  CINEMA WALL",
                    color = ProjectorIvory,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp,
                )
            }

            Text(
                "${(index + 1).toString().padStart(2, '0')}  /  ${resourceIds.size.toString().padStart(2, '0')}",
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 15.dp),
                color = ProjectorIvory.copy(alpha = .72f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )

            Column(
                Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 70.dp, bottom = 17.dp)
            ) {
                Text(
                    "FEATURED FRAME",
                    color = RecRed,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "“${heroQuotes[quoteIndex]}”",
                    color = ProjectorIvory,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                Modifier.align(Alignment.BottomEnd).padding(end = 15.dp, bottom = 17.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(resourceIds.size) { dot ->
                    Box(
                        Modifier.width(if (dot == index) 16.dp else 4.dp).height(3.dp)
                            .background(
                                if (dot == index) RecRed else ProjectorIvory.copy(alpha = .38f),
                                RoundedCornerShape(10.dp),
                            )
                    )
                }
            }
        }
    }
}

'''

p.write_text(s[:start] + replacement + s[end:])
