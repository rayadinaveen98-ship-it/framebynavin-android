from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app"

OLD_VARIANTS = ("FrameByNavin", "Frame by Navin", "FRAME BY NAVIN")
TECHNICAL_ASSIGNMENT = re.compile(
    r"\b(?:PREF|PREFERENCES|KEY|FOLDER|ROOT|PATH|DATABASE|DB|BUCKET|COLLECTION|SCHEMA|AUTHORITY|ACTION|CHANNEL_ID|URI|FILE_ID)\b",
    re.IGNORECASE,
)
STRING_LITERAL = re.compile(r'"(?:\\.|[^"\\])*"')


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    if old not in text:
        raise RuntimeError(f"v133 migration could not find {label}")
    return text.replace(old, new, 1)


# -----------------------------------------------------------------------------
# Versioning: v131 remains the immutable stable checkpoint; v133 is Backlot test.
# -----------------------------------------------------------------------------
gradle_path = "app/build.gradle.kts"
gradle = read(gradle_path)
gradle = replace_once(gradle, "versionCode = 131", "versionCode = 133", "versionCode")
gradle = replace_once(
    gradle,
    'versionName = "2.0.0-rc7-cine-pulse-motion-pass"',
    'versionName = "2.0.0-rc8-backlot-brand-system"',
    "versionName",
)
write(gradle_path, gradle)


# -----------------------------------------------------------------------------
# Production vector identity. These are real Android vector resources, not a
# screenshot baked into the app. Geometry follows the approved cinematic B:
# film-strip spine + architectural B + dark doorway + warm floor light.
# -----------------------------------------------------------------------------
CINEMATIC = r'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:pathData="M13,5 H95 C100,5 103,8 103,13 V95 C103,100 100,103 95,103 H13 C8,103 5,100 5,95 V13 C5,8 8,5 13,5 Z">
        <aapt:attr name="android:fillColor"><gradient android:startX="16" android:startY="8" android:endX="94" android:endY="102" android:type="linear"><item android:offset="0" android:color="#FF07101A"/><item android:offset="0.52" android:color="#FF030507"/><item android:offset="1" android:color="#FF071729"/></gradient></aapt:attr>
    </path>
    <path android:pathData="M12,101 L61,78 L97,101 Z" android:fillColor="#334A9BFF"/>
    <path android:pathData="M11,101 L61,78 L68,101 Z">
        <aapt:attr name="android:fillColor"><gradient android:startX="60" android:startY="78" android:endX="43" android:endY="103" android:type="linear"><item android:offset="0" android:color="#FFFFF0B5"/><item android:offset="0.36" android:color="#FFFFB239"/><item android:offset="1" android:color="#00F28C1E"/></gradient></aapt:attr>
    </path>
    <path android:pathData="M31,19 H43 V88 H31 Z">
        <aapt:attr name="android:fillColor"><gradient android:startX="31" android:startY="19" android:endX="43" android:endY="88" android:type="linear"><item android:offset="0" android:color="#FFFFF4D2"/><item android:offset="0.50" android:color="#FFF3BB58"/><item android:offset="1" android:color="#FFB96824"/></gradient></aapt:attr>
    </path>
    <path android:pathData="M34,25 H40 V31 H34 Z M34,36 H40 V42 H34 Z M34,47 H40 V53 H34 Z M34,58 H40 V64 H34 Z M34,69 H40 V75 H34 Z M34,80 H40 V86 H34 Z" android:fillColor="#FF11100E"/>
    <path android:pathData="M45,19 L61,23 C77,26 84,34 84,45 C84,54 79,59 72,62 C82,65 88,73 88,82 C88,94 79,99 61,99 L45,99 L45,86 L59,86 C69,86 74,83 74,77 C74,70 69,67 59,67 H45 V55 H58 C67,55 72,52 72,46 C72,40 68,37 59,35 L45,32 Z">
        <aapt:attr name="android:fillColor"><gradient android:startX="45" android:startY="19" android:endX="86" android:endY="99" android:type="linear"><item android:offset="0" android:color="#FFFFF4D7"/><item android:offset="0.40" android:color="#FFFFD27A"/><item android:offset="0.73" android:color="#FFE3A04A"/><item android:offset="1" android:color="#FF7D91AA"/></gradient></aapt:attr>
    </path>
    <path android:pathData="M45,32 L61,42 L61,78 L45,86 Z" android:fillColor="#FF050607"/>
    <path android:pathData="M61,42 L66,46 L66,75 L61,78 Z" android:fillColor="#FFFFC45D" android:fillAlpha="0.55"/>
    <path android:pathData="M95,7 C101,10 102,13 102,21 V91" android:fillColor="#00000000" android:strokeColor="#FF54B8FF" android:strokeAlpha="0.75" android:strokeWidth="0.8"/>
    <path android:pathData="M13,102 H61" android:fillColor="#00000000" android:strokeColor="#FFFF9C2E" android:strokeAlpha="0.85" android:strokeWidth="0.8"/>
</vector>'''

MINIMAL = r'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:pathData="M13,5 H95 C100,5 103,8 103,13 V95 C103,100 100,103 95,103 H13 C8,103 5,100 5,95 V13 C5,8 8,5 13,5 Z" android:fillColor="#FF060708"/>
    <path android:pathData="M30,20 H41 V88 H30 Z" android:fillColor="#FFF2C46F"/>
    <path android:pathData="M33,27 H38 V33 H33 Z M33,39 H38 V45 H33 Z M33,51 H38 V57 H33 Z M33,63 H38 V69 H33 Z M33,75 H38 V81 H33 Z" android:fillColor="#FF090A0B"/>
    <path android:pathData="M44,20 H60 C77,20 84,29 84,41 C84,50 79,56 71,59 C82,62 88,70 88,81 C88,94 79,99 60,99 H44 V86 H59 C69,86 74,83 74,77 C74,70 69,67 59,67 H44 V54 H58 C67,54 72,51 72,44 C72,37 67,33 58,33 H44 Z" android:fillColor="#FFF2C46F"/>
    <path android:pathData="M44,31 L60,41 L60,79 L44,87 Z" android:fillColor="#FF060708"/>
</vector>'''

LIGHT = r'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:pathData="M13,5 H95 C100,5 103,8 103,13 V95 C103,100 100,103 95,103 H13 C8,103 5,100 5,95 V13 C5,8 8,5 13,5 Z" android:fillColor="#FFFFF8E9"/>
    <path android:pathData="M30,20 H41 V88 H30 Z" android:fillColor="#FF141518"/>
    <path android:pathData="M33,27 H38 V33 H33 Z M33,39 H38 V45 H33 Z M33,51 H38 V57 H33 Z M33,63 H38 V69 H33 Z M33,75 H38 V81 H33 Z" android:fillColor="#FFFFF8E9"/>
    <path android:pathData="M44,20 H60 C77,20 84,29 84,41 C84,50 79,56 71,59 C82,62 88,70 88,81 C88,94 79,99 60,99 H44 V86 H59 C69,86 74,83 74,77 C74,70 69,67 59,67 H44 V54 H58 C67,54 72,51 72,44 C72,37 67,33 58,33 H44 Z" android:fillColor="#FF141518"/>
    <path android:pathData="M44,31 L60,41 L60,79 L44,87 Z" android:fillColor="#FFFFF8E9"/>
    <path android:pathData="M13,102 H95" android:fillColor="#00000000" android:strokeColor="#FFD79B3C" android:strokeWidth="1"/>
</vector>'''

HORIZONTAL = r'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:pathData="M13,5 H95 C100,5 103,8 103,13 V95 C103,100 100,103 95,103 H13 C8,103 5,100 5,95 V13 C5,8 8,5 13,5 Z" android:fillColor="#FF05070A"/>
    <path android:pathData="M19,28 H27 V79 H19 Z" android:fillColor="#FFF1C16B"/>
    <path android:pathData="M21,33 H25 V38 H21 Z M21,43 H25 V48 H21 Z M21,53 H25 V58 H21 Z M21,63 H25 V68 H21 Z M21,73 H25 V77 H21 Z" android:fillColor="#FF08090A"/>
    <path android:pathData="M30,28 H41 C52,28 57,34 57,42 C57,48 53,52 49,54 C56,56 60,61 60,68 C60,76 54,79 41,79 H30 V70 H40 C47,70 50,68 50,64 C50,60 47,58 40,58 H30 V50 H39 C46,50 49,48 49,44 C49,40 46,38 39,38 H30 Z" android:fillColor="#FFF1C16B"/>
    <path android:pathData="M30,37 L40,44 L40,64 L30,71 Z" android:fillColor="#FF05070A"/>
    <path android:pathData="M66,39 H91 V43 H66 Z M66,51 H86 V55 H66 Z M66,63 H94 V67 H66 Z" android:fillColor="#FFF1C16B"/>
</vector>'''

for name, xml in {
    "backlot_icon_cinematic.xml": CINEMATIC,
    "backlot_icon_minimal.xml": MINIMAL,
    "backlot_icon_light.xml": LIGHT,
    "backlot_icon_horizontal.xml": HORIZONTAL,
}.items():
    write(f"app/src/main/res/drawable/{name}", xml + "\n")


# -----------------------------------------------------------------------------
# Launcher aliases. Keep applicationId/com.framebynavin.app unchanged so Backlot
# upgrades the existing installation instead of creating a second app.
# -----------------------------------------------------------------------------
manifest_path = "app/src/main/AndroidManifest.xml"
manifest = read(manifest_path)
manifest = manifest.replace('android:icon="@drawable/ic_framebynavin_launcher"', 'android:icon="@drawable/backlot_icon_cinematic"')
manifest = manifest.replace('android:roundIcon="@drawable/ic_framebynavin_launcher"', 'android:roundIcon="@drawable/backlot_icon_cinematic"')
manifest = manifest.replace('android:label="FrameByNavin"', 'android:label="Backlot"', 1)
for suffix in (
    "Compact", "Creator Desk", "Quick Idea", "New Project", "Current Project",
    "Next Reminder", "Content Calendar", "Daily Brief", "Creator Insights",
):
    manifest = manifest.replace(f'android:label="FrameByNavin {suffix}"', f'android:label="Backlot {suffix}"')

old_main = '''        <activity android:name=".MainActivity" android:exported="true" android:launchMode="singleTop" android:theme="@style/Theme.FrameByNavin.Starting">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>'''
new_main = '''        <activity android:name=".MainActivity" android:exported="true" android:launchMode="singleTop" android:theme="@style/Theme.FrameByNavin.Starting" />

        <!-- Backlot launcher identity. Exactly one alias is enabled at first install. -->
        <activity-alias
            android:name=".launcher.BacklotCinematicAlias"
            android:enabled="true"
            android:exported="true"
            android:icon="@drawable/backlot_icon_cinematic"
            android:label="Backlot"
            android:targetActivity=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity-alias>
        <activity-alias
            android:name=".launcher.BacklotMinimalAlias"
            android:enabled="false"
            android:exported="true"
            android:icon="@drawable/backlot_icon_minimal"
            android:label="Backlot"
            android:targetActivity=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity-alias>
        <activity-alias
            android:name=".launcher.BacklotLightAlias"
            android:enabled="false"
            android:exported="true"
            android:icon="@drawable/backlot_icon_light"
            android:label="Backlot"
            android:targetActivity=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity-alias>
        <activity-alias
            android:name=".launcher.BacklotHorizontalAlias"
            android:enabled="false"
            android:exported="true"
            android:icon="@drawable/backlot_icon_horizontal"
            android:label="Backlot"
            android:targetActivity=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity-alias>'''
manifest = replace_once(manifest, old_main, new_main, "MainActivity launcher conversion")
write(manifest_path, manifest)


# -----------------------------------------------------------------------------
# Runtime icon switching + Settings UI.
# -----------------------------------------------------------------------------
ICON_KT = r'''package com.framebynavin.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.R
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory

internal enum class BacklotAppIcon(
    val displayName: String,
    val description: String,
    @DrawableRes val previewRes: Int,
    val aliasSuffix: String,
) {
    CINEMATIC("Cinematic", "The signature illuminated Backlot gateway.", R.drawable.backlot_icon_cinematic, "BacklotCinematicAlias"),
    MINIMAL("Minimal", "Reduced gold mark for a quieter home screen.", R.drawable.backlot_icon_minimal, "BacklotMinimalAlias"),
    LIGHT("Light", "Ivory edition with the dark Backlot mark.", R.drawable.backlot_icon_light, "BacklotLightAlias"),
    HORIZONTAL("Horizontal", "A compact wide-lockup inspired edition.", R.drawable.backlot_icon_horizontal, "BacklotHorizontalAlias"),
    ;

    fun componentName(packageName: String): String = "$packageName.launcher.$aliasSuffix"
}

internal object BacklotAppIconManager {
    private const val PREFS = "backlot_launcher_identity"
    private const val KEY = "selected_icon"

    fun current(context: Context): BacklotAppIcon {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return BacklotAppIcon.entries.firstOrNull { it.name == stored } ?: BacklotAppIcon.CINEMATIC
    }

    fun set(context: Context, icon: BacklotAppIcon) {
        val appContext = context.applicationContext
        val packageManager = appContext.packageManager

        // Enable the destination first so there is never a moment with no launcher entry.
        packageManager.setComponentEnabledSetting(
            ComponentName(appContext, icon.componentName(appContext.packageName)),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        BacklotAppIcon.entries.filterNot { it == icon }.forEach { candidate ->
            packageManager.setComponentEnabledSetting(
                ComponentName(appContext, candidate.componentName(appContext.packageName)),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, icon.name).apply()
    }
}

@Composable
internal fun V133AppIconPicker() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(BacklotAppIconManager.current(context)) }

    Column(Modifier.fillMaxWidth()) {
        Text("APP ICON", color = MutedGold, fontSize = 8.6.sp, fontWeight = FontWeight.Black, letterSpacing = 1.05.sp)
        Spacer(Modifier.height(4.dp))
        Text("Choose how Backlot appears on your home screen.", color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Black)
        Text("Your launcher may take a moment to refresh after switching.", color = MutedText, fontSize = 8.3.sp)
        Spacer(Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BacklotAppIcon.entries.chunked(2).forEach { rowIcons ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowIcons.forEach { icon ->
                        val active = icon == selected
                        Surface(
                            modifier = Modifier.weight(1f).clickable {
                                runCatching { BacklotAppIconManager.set(context, icon) }
                                    .onSuccess {
                                        selected = icon
                                        Toast.makeText(context, "Backlot icon: ${icon.displayName}", Toast.LENGTH_SHORT).show()
                                    }
                                    .onFailure {
                                        Toast.makeText(context, "Could not change icon on this launcher.", Toast.LENGTH_SHORT).show()
                                    }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = CinemaSurface,
                            border = BorderStroke(1.dp, if (active) MutedGold else CinemaLine),
                        ) {
                            Column(Modifier.padding(11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                    Image(painterResource(icon.previewRes), contentDescription = "${icon.displayName} Backlot icon", modifier = Modifier.size(54.dp))
                                }
                                Spacer(Modifier.height(7.dp))
                                Text(icon.displayName, color = ProjectorIvory, fontSize = 9.7.sp, fontWeight = FontWeight.Bold)
                                Text(icon.description, color = MutedText, fontSize = 7.2.sp, lineHeight = 9.2.sp, maxLines = 2)
                            }
                        }
                    }
                    if (rowIcons.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
'''
write("app/src/main/java/com/framebynavin/app/ui/V133BacklotAppIcon.kt", ICON_KT + "\n")

appearance_path = "app/src/main/java/com/framebynavin/app/ui/V127AppearanceSettings.kt"
appearance = read(appearance_path)
needle = '''        Spacer(Modifier.height(11.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = CinemaSurface,
            border = BorderStroke(1.dp, CinemaLine),
        ) {'''
replacement = '''        Spacer(Modifier.height(16.dp))
        V133AppIconPicker()

        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = CinemaSurface,
            border = BorderStroke(1.dp, CinemaLine),
        ) {'''
appearance = replace_once(appearance, needle, replacement, "Appearance app icon picker")
write(appearance_path, appearance)


# -----------------------------------------------------------------------------
# Welcome ident: preserve the approved 5s cinematic timing/threads, replace the
# old mark with the production Backlot gateway and new wordmark.
# -----------------------------------------------------------------------------
welcome_path = "app/src/main/java/com/framebynavin/app/ui/V174CinematicWelcome.kt"
welcome = read(welcome_path)
welcome = welcome.replace("import androidx.compose.ui.geometry.Offset\n", "import androidx.compose.ui.geometry.CornerRadius\nimport androidx.compose.ui.geometry.Offset\n")
welcome = welcome.replace("import androidx.compose.ui.graphics.graphicsLayer\n", "import androidx.compose.ui.graphics.graphicsLayer\nimport androidx.compose.ui.graphics.drawscope.Stroke\n")
welcome = welcome.replace("FrameByNavinIdentMark(", "BacklotIdentMark(")
welcome = welcome.replace('text = "FRAME BY NAVIN"', 'text = "BACKLOT"')
welcome = welcome.replace("fontSize = 27.6.sp,", "fontSize = 29.2.sp,")
welcome = welcome.replace("letterSpacing = 3.2.sp,", "letterSpacing = 5.6.sp,")
welcome = welcome.replace(".offset(y = 84.dp)", ".offset(y = 83.dp)")
welcome = welcome.replace(".offset(y = 111.dp)", ".offset(y = 133.dp)")

# Insert the locked tagline below BACKLOT before the light sweep.
tagline_anchor = '''        if (sweep.value > 0f) {'''
tagline = '''        Text(
            text = "CREATE WHAT'S NEXT",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 116.dp)
                .alpha(title.value),
            color = MutedGold.copy(alpha = .92f),
            fontSize = 8.6.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.5.sp,
            textAlign = TextAlign.Center,
        )

        if (sweep.value > 0f) {'''
welcome = replace_once(welcome, tagline_anchor, tagline, "Backlot welcome tagline")

# Replace the old mark implementation from its private function to EOF.
mark_index = welcome.find("@Composable\nprivate fun BacklotIdentMark(")
if mark_index < 0:
    raise RuntimeError("Backlot mark function boundary not found")
new_mark = r'''@Composable
private fun BacklotIdentMark(
    modifier: Modifier = Modifier,
    reveal: Float,
) {
    Canvas(modifier) {
        val s = size.minDimension
        fun x(v: Float) = (v / 108f) * s
        fun y(v: Float) = (v / 108f) * s

        // Atmospheric blue/gold edge light from the approved Backlot concept.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFAD3D).copy(alpha = .25f * reveal),
                    Color(0xFF234D77).copy(alpha = .10f * reveal),
                    Color.Transparent,
                ),
                center = Offset(x(55f), y(58f)),
                radius = x(55f),
            ),
            radius = x(55f),
            center = Offset(x(55f), y(58f)),
        )

        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color(0xFF08111B), Color(0xFF020304), Color(0xFF061426)),
                start = Offset(x(8f), y(7f)),
                end = Offset(x(101f), y(103f)),
            ),
            topLeft = Offset(x(7f), y(7f)),
            size = Size(x(94f), y(94f)),
            cornerRadius = CornerRadius(x(19f), y(19f)),
        )
        drawRoundRect(
            color = Color(0xFF3FAEFF).copy(alpha = .40f * reveal),
            topLeft = Offset(x(7.5f), y(7.5f)),
            size = Size(x(93f), y(93f)),
            cornerRadius = CornerRadius(x(18.5f), y(18.5f)),
            style = Stroke(width = x(.75f)),
        )

        val beam = Path().apply {
            moveTo(x(62f), y(78f)); lineTo(x(16f), y(100f)); lineTo(x(96f), y(100f)); close()
        }
        drawPath(
            beam,
            Brush.linearGradient(
                listOf(Color(0x00FF9A24), Color(0xAAFFAA32), Color(0xFFFFE0A0), Color(0x2254B8FF)),
                start = Offset(x(18f), y(101f)),
                end = Offset(x(77f), y(79f)),
            ),
        )

        drawRect(
            brush = Brush.linearGradient(
                listOf(Color(0xFFFFF3D0), Color(0xFFF2BE60), Color(0xFFB76625)),
                start = Offset(x(31f), y(19f)),
                end = Offset(x(43f), y(88f)),
            ),
            topLeft = Offset(x(31f), y(19f)),
            size = Size(x(12f), y(69f)),
        )
        repeat(6) { i ->
            drawRoundRect(
                color = Color(0xFF11100E),
                topLeft = Offset(x(34f), y(25f + i * 11f)),
                size = Size(x(6f), y(6f)),
                cornerRadius = CornerRadius(x(1.1f), x(1.1f)),
            )
        }

        val b = Path().apply {
            moveTo(x(45f), y(19f))
            lineTo(x(61f), y(23f))
            cubicTo(x(77f), y(26f), x(84f), y(34f), x(84f), y(45f))
            cubicTo(x(84f), y(54f), x(79f), y(59f), x(72f), y(62f))
            cubicTo(x(82f), y(65f), x(88f), y(73f), x(88f), y(82f))
            cubicTo(x(88f), y(94f), x(79f), y(99f), x(61f), y(99f))
            lineTo(x(45f), y(99f)); lineTo(x(45f), y(86f)); lineTo(x(59f), y(86f))
            cubicTo(x(69f), y(86f), x(74f), y(83f), x(74f), y(77f))
            cubicTo(x(74f), y(70f), x(69f), y(67f), x(59f), y(67f))
            lineTo(x(45f), y(67f)); lineTo(x(45f), y(55f)); lineTo(x(58f), y(55f))
            cubicTo(x(67f), y(55f), x(72f), y(52f), x(72f), y(46f))
            cubicTo(x(72f), y(40f), x(68f), y(37f), x(59f), y(35f))
            lineTo(x(45f), y(32f)); close()
        }
        drawPath(
            b,
            Brush.linearGradient(
                listOf(Color(0xFFFFF5DB), Color(0xFFFFD27A), Color(0xFFE3A04A), Color(0xFF7896B4)),
                start = Offset(x(45f), y(19f)),
                end = Offset(x(88f), y(99f)),
            ),
        )

        val doorway = Path().apply {
            moveTo(x(45f), y(32f)); lineTo(x(61f), y(42f)); lineTo(x(61f), y(78f)); lineTo(x(45f), y(86f)); close()
        }
        drawPath(doorway, Color(0xFF040506))
        val doorLight = Path().apply {
            moveTo(x(61f), y(42f)); lineTo(x(66f), y(46f)); lineTo(x(66f), y(75f)); lineTo(x(61f), y(78f)); close()
        }
        drawPath(doorLight, Color(0xFFFFC45D).copy(alpha = .58f * reveal))
    }
}
'''
welcome = welcome[:mark_index] + new_mark
welcome = welcome.replace("FrameByNavin founder ident", "Backlot founder ident")
welcome = welcome.replace("FrameByNavin's own", "Backlot's own")
write(welcome_path, welcome)


# Also update the dormant alternate brand welcome so no old product copy can leak back later.
alt_welcome = ROOT / "app/src/main/java/com/framebynavin/app/ui/V133BrandWelcome.kt"
if alt_welcome.exists():
    text = alt_welcome.read_text(encoding="utf-8")
    text = text.replace('"FRAME BY NAVIN"', '"BACKLOT"')
    text = text.replace('"CREATOR CONTROL ROOM"', '"CREATE WHAT\'S NEXT"')
    alt_welcome.write_text(text, encoding="utf-8")


# -----------------------------------------------------------------------------
# Comprehensive user-facing rename audit.
# We replace brand names inside Kotlin string literals unless the same line is a
# clearly persistent/technical identifier. Class names, package names, themes,
# storage keys and existing data roots intentionally remain FrameByNavin.
# -----------------------------------------------------------------------------
changed_strings = []
preserved_technical = []


def replace_literal_brand(line: str, path: Path, line_no: int) -> str:
    if not any(v in line for v in OLD_VARIANTS):
        return line

    assignment_prefix = line.split("=", 1)[0] if "=" in line else ""
    persistent_api = any(token in line for token in (
        "getSharedPreferences(", "preferencesKey(", "stringPreferencesKey(",
        "intPreferencesKey(", "booleanPreferencesKey(", "File(", "Uri.parse(",
    ))
    technical = bool(TECHNICAL_ASSIGNMENT.search(assignment_prefix)) or persistent_api

    def repl(match: re.Match) -> str:
        literal = match.group(0)
        if not any(v in literal for v in OLD_VARIANTS):
            return literal
        if technical:
            preserved_technical.append(f"{path.relative_to(ROOT)}:{line_no}: {literal}")
            return literal
        updated = literal
        updated = updated.replace("FRAME BY NAVIN", "BACKLOT")
        updated = updated.replace("Frame by Navin", "Backlot")
        updated = updated.replace("FrameByNavin", "Backlot")
        if updated != literal:
            changed_strings.append(f"{path.relative_to(ROOT)}:{line_no}: {literal} -> {updated}")
        return updated

    return STRING_LITERAL.sub(repl, line)


for path in sorted((APP / "src" / "main").rglob("*.kt")):
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    new_lines = [replace_literal_brand(line, path, idx + 1) for idx, line in enumerate(lines)]
    if new_lines != lines:
        path.write_text("".join(new_lines), encoding="utf-8")

# XML user-facing attributes/text. Explicitly protect technical style/resource names.
for path in sorted((APP / "src" / "main").rglob("*.xml")):
    if path.name == "AndroidManifest.xml":
        continue
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    out = []
    for idx, line in enumerate(lines, 1):
        updated = line
        if any(v in line for v in OLD_VARIANTS) and "Theme.FrameByNavin" not in line and "ic_framebynavin" not in line:
            if any(attr in line for attr in ("android:text=", "android:label=", "android:contentDescription=", "tools:text=")):
                updated = updated.replace("FRAME BY NAVIN", "BACKLOT").replace("Frame by Navin", "Backlot").replace("FrameByNavin", "Backlot")
                if updated != line:
                    changed_strings.append(f"{path.relative_to(ROOT)}:{idx}: XML brand copy")
        out.append(updated)
    if out != lines:
        path.write_text("".join(out), encoding="utf-8")

# Manifest was patched manually; ensure any remaining widget/application labels are Backlot.
manifest = read(manifest_path)
manifest = re.sub(r'android:label="FrameByNavin([^\"]*)"', lambda m: f'android:label="Backlot{m.group(1)}"', manifest)
write(manifest_path, manifest)


# -----------------------------------------------------------------------------
# Final audit: any OLD brand inside a likely user-facing quoted string is a hard
# failure. Technical identifiers are reported but allowed.
# -----------------------------------------------------------------------------
violations = []
remaining = []
for path in sorted((APP / "src" / "main").rglob("*")):
    if not path.is_file() or path.suffix not in {".kt", ".xml", ".java"}:
        continue
    for idx, line in enumerate(path.read_text(encoding="utf-8", errors="ignore").splitlines(), 1):
        if not any(v in line for v in OLD_VARIANTS):
            continue
        rel = path.relative_to(ROOT)
        remaining.append(f"{rel}:{idx}: {line.strip()}")
        if path.suffix == ".xml":
            if "Theme.FrameByNavin" in line or "ic_framebynavin" in line:
                continue
            if any(attr in line for attr in ("android:label=", "android:text=", "android:contentDescription=", "tools:text=")):
                violations.append(f"{rel}:{idx}: {line.strip()}")
            continue
        assignment_prefix = line.split("=", 1)[0] if "=" in line else ""
        technical = bool(TECHNICAL_ASSIGNMENT.search(assignment_prefix)) or any(token in line for token in (
            "getSharedPreferences(", "preferencesKey(", "stringPreferencesKey(", "File(", "Uri.parse(",
        ))
        quoted_old = any(any(v in literal for v in OLD_VARIANTS) for literal in STRING_LITERAL.findall(line))
        if quoted_old and not technical:
            violations.append(f"{rel}:{idx}: {line.strip()}")

report = [
    "BACKLOT BRAND MIGRATION AUDIT",
    "============================",
    f"User-facing string replacements: {len(changed_strings)}",
    f"Protected technical literals: {len(preserved_technical)}",
    f"Remaining old-brand source lines (mostly identifiers/comments/themes): {len(remaining)}",
    f"User-facing violations: {len(violations)}",
    "",
    "CHANGED USER-FACING COPY",
    *changed_strings,
    "",
    "PRESERVED TECHNICAL LITERALS",
    *preserved_technical,
    "",
    "ALL REMAINING OLD-BRAND SOURCE LINES",
    *remaining,
]
write("build/backlot-brand-audit.txt", "\n".join(report) + "\n")

if violations:
    raise RuntimeError("Old user-facing FrameByNavin branding remains:\n" + "\n".join(violations))

print("v133 Backlot brand system applied")
print(f"renamed user-facing strings: {len(changed_strings)}")
print(f"preserved technical literals: {len(preserved_technical)}")
