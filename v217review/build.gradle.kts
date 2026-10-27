plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

/**
 * A SAFE side-by-side companion review application. Compiles the exact V217
 * production chooser, Settings picker, theme and renderer source from :app,
 * without carrying its Firebase, reminders, backup, permissions or data.
 *
 * Do NOT merge this review utility into the Backlot production APK or
 * treat it as a full Backlot acceptance build.
 */
val realCompanionSources = listOf(
    "com/framebynavin/app/ui/V144GuideSystem.kt",
    "com/framebynavin/app/ui/V144RasterGuideCharacter.kt",
    "com/framebynavin/app/ui/V129CinePulseGuide.kt",
    "com/framebynavin/app/ui/V217CompanionConceptPreview.kt",
    "com/framebynavin/app/ui/V217LiveCompanionCharacter.kt",
    "com/framebynavin/app/ui/theme/FrameByNavinTheme.kt",
)
val sharedArt = listOf(
    "drawable-nodpi/guide_funny_welcome.webp",
    "drawable-nodpi/guide_funny_point.webp",
    "drawable-nodpi/guide_funny_present.webp",
    "drawable-nodpi/guide_funny_thinking.webp",
    "drawable-nodpi/guide_funny_celebrate.webp",
    "drawable-nodpi/guide_cute_welcome.webp",
    "drawable-nodpi/guide_v217_frame_concept.webp",
    "drawable-nodpi/guide_v217_navi_concept.webp",
    "drawable-nodpi/guide_v217_cute_concept.webp",
)
val prepareReviewSources = tasks.register<Copy>("prepareReviewSources") {
    from(rootProject.file("app/src/main/java")) {
        realCompanionSources.forEach { include(it) }
    }
    into(layout.buildDirectory.dir("generated/real-companion-sources"))
}
val prepareReviewArt = tasks.register<Copy>("prepareReviewArt") {
    from(rootProject.file("app/src/main/res")) {
        sharedArt.forEach { include(it) }
    }
    into(layout.buildDirectory.dir("generated/real-companion-res"))
}

android {
    namespace = "com.framebynavin.app"
    compileSdk = 36

    defaultConfig {
        // Unique package means it CANNOT overwrite installed Backlot data.
        applicationId = "com.framebynavin.v217review"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "217-review.1"
    }
    sourceSets.getByName("main").java.srcDir(layout.buildDirectory.dir("generated/real-companion-sources"))
    sourceSets.getByName("main").res.srcDir(layout.buildDirectory.dir("generated/real-companion-res"))
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

tasks.named("preBuild") {
    dependsOn(prepareReviewSources, prepareReviewArt)
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.material3:material3")
    // No Firebase, no internet/network permission, no WorkManager, no alarm
    // scheduling, no media recorder, no creator databases.
}
