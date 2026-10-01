from pathlib import Path

path = Path("app/src/test/java/com/framebynavin/app/ui/V144GuideSystemTest.kt")
text = path.read_text()
old = '''    @Test
    fun `cute resolves every semantic state to a real raster asset`() {
        CinePulseState.entries.forEach { state ->
            assertNotEquals(0, v144GuideDrawable(V144GuideIdentity.CUTE, state))
        }
        assertEquals(R.drawable.guide_cute_welcome, v144GuideDrawable(V144GuideIdentity.CUTE, CinePulseState.WAVE))
        assertEquals(R.drawable.guide_cute_listening, v144GuideDrawable(V144GuideIdentity.CUTE, CinePulseState.LOOK))
        assertEquals(R.drawable.guide_cute_thinking, v144GuideDrawable(V144GuideIdentity.CUTE, CinePulseState.THINK))
        assertEquals(R.drawable.guide_cute_celebrate, v144GuideDrawable(V144GuideIdentity.CUTE, CinePulseState.SUCCESS))
        assertEquals(R.drawable.guide_cute_celebrate, v144GuideDrawable(V144GuideIdentity.CUTE, CinePulseState.CELEBRATE))
    }
'''
new = '''    @Test
    fun `cute uses the validated welcome master for every semantic state`() {
        CinePulseState.entries.forEach { state ->
            assertEquals(
                "Cute must stay on the validated non-deformed master asset for $state",
                R.drawable.guide_cute_welcome,
                v144GuideDrawable(V144GuideIdentity.CUTE, state),
            )
        }
    }
'''
if text.count(old) != 1:
    raise SystemExit("Expected the original Cute guide mapping test exactly once")
path.write_text(text.replace(old, new, 1))
print("V147 guide regression test updated for validated Cute fallback")
