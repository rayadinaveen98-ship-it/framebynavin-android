from pathlib import Path


def replace_once(path: str, old: str, new: str, label: str) -> None:
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one anchor in {path}, got {count}")
    p.write_text(text.replace(old, new, 1))


replace_once(
    "app/build.gradle.kts",
    'versionCode = 103\n        versionName = "2.0.0-alpha1.2n-creative-intelligence"',
    'versionCode = 104\n        versionName = "2.0.0-alpha1.2o-gemini-foundation"',
    "version bump",
)

# Platform analytics are creator-private even though their values are structured/non-secret.
replace_once(
    "app/src/main/java/com/framebynavin/app/data/CreatorAiEvidencePack.kt",
    '''    private fun public(id: String, klass: CreatorAiEvidenceClass, label: String, value: String) =\n        CreatorAiEvidence(id, klass, CreatorAiEvidenceVisibility.PUBLIC, label, value)''',
    '''    private fun public(id: String, klass: CreatorAiEvidenceClass, label: String, value: String) =\n        CreatorAiEvidence(\n            id,\n            klass,\n            if (klass == CreatorAiEvidenceClass.PLATFORM_ANALYTICS) CreatorAiEvidenceVisibility.PRIVATE_CREATOR_DATA\n            else CreatorAiEvidenceVisibility.PUBLIC,\n            label,\n            value,\n        )''',
    "analytics visibility",
)

ui = "app/src/main/java/com/framebynavin/app/ui/V20VideoPostmortemUi.kt"
anchor = '''            Text(\n                "Project/stage duration is elapsed residence time, not active editing or research time.",\n                color = MutedText.copy(alpha = .72f),\n                fontSize = 7.2.sp,\n                lineHeight = 10.sp,\n            )'''
replace_once(
    ui,
    anchor,
    anchor + '''\n\n            Spacer(Modifier.height(10.dp))\n            V20GeminiIntelligenceCard(postmortem)''',
    "Gemini card integration",
)

print("Materialized v104 Gemini Intelligence foundation")
