from pathlib import Path

# Compatibility shim for the Compose UI test API used by this Android build.
TEST = Path('app/src/androidTest/java/com/framebynavin/app/ui/V18CinematicHomeHeroAlpha16UiTest.kt')
text = TEST.read_text()
old_import = 'import androidx.compose.ui.test.onNode\n'
if text.count(old_import) != 1:
    raise SystemExit(f'alpha16 test import: expected one match, found {text.count(old_import)}')
TEST.write_text(text.replace(old_import, '', 1))

# Alpha16 intentionally retires the old standalone "BEST FRAMES OF TODAY" entry.
# Update the historical release-blocker so it now protects the unified cinematic hero contract.
LEGACY = Path('app/src/androidTest/java/com/framebynavin/app/ui/V132ReleaseBlockerUiTest.kt')
legacy = LEGACY.read_text()
old_test = '''    @Test
    fun heroSlideshow_rendersOriginalBestFramesEntry() {
        composeRule.setContent { V131HomeHeroSlideshow() }

        composeRule.onNodeWithText("BEST FRAMES OF TODAY").assertIsDisplayed()
        composeRule.onNodeWithText("Tap to select up to 10 original images").assertIsDisplayed()
        composeRule.onNodeWithText("YOUR CINEMA WALL").assertDoesNotExist()
    }
'''
new_test = '''    @Test
    fun homeHero_rendersUnifiedAlpha16GreetingEntry() {
        composeRule.setContent { V18CinematicHomeHero("King") }

        composeRule.onNodeWithText("FRAME BY NAVIN").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add slideshow images").assertIsDisplayed()
        composeRule.onNodeWithText("BEST FRAMES OF TODAY").assertDoesNotExist()
    }
'''
if legacy.count(old_test) != 1:
    raise SystemExit(f'legacy hero release blocker: expected one match, found {legacy.count(old_test)}')
LEGACY.write_text(legacy.replace(old_test, new_test, 1))

print('Applied Alpha16 Compose + legacy hero release-blocker compatibility fix')
