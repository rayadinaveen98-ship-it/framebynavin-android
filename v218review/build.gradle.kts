plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

/**
 * Compiles the REAL Backlot V218 wave renderer and policies unchanged,
 * but has no dependency on the production app, data or reminder services.
 * Separate package ID keeps the owner's installed Backlot safe.
 */
val sharedWaveSources = listOf(
    "com/framebynavin/app/reminders/V195ReminderParticleScene.kt",
    "com/framebynavin/app/reminders/V195ParticleGeometry.kt",
    "com/framebynavin/app/reminders/V197AmbientGeometry.kt",
    "com/framebynavin/app/reminders/V198ParticleMotion.kt",
    "com/framebynavin/app/reminders/V202PremiumParticleMotion.kt",
    "com/framebynavin/app/reminders/V218UnifiedWavePolicy.kt",
)
val stageWaveSources = tasks.register<Copy>("stageWaveSources") {
    from(rootProject.file("app/src/main/java")) {
        sharedWaveSources.forEach { include(it) }
    }
    into(layout.buildDirectory.dir("generated/real-v218-sources"))
}

android {
    namespace = "com.framebynavin.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.framebynavin.v218review"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "218-review.1"
    }
    sourceSets.getByName("main").java.srcDir(layout.buildDirectory.dir("generated/real-v218-sources"))
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}
tasks.named("preBuild") { dependsOn(stageWaveSources) }

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    // No Firebase, network, alarm scheduler, media recorder, database, or accounts.
}
