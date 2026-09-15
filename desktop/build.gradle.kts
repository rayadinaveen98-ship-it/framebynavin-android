import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.4.20"
    id("org.jetbrains.compose") version "1.12.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
}

group = "com.backlot"
version = "0.1.1"

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

compose.desktop {
    application {
        mainClass = "com.backlot.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Backlot"
            packageVersion = "0.1.1"
            description = "Backlot creator workspace"
            vendor = "Backlot"

            windows {
                // Keep this UUID stable across Backlot desktop upgrades.
                upgradeUuid = "641dd83e-96cf-4e82-8c0b-70e8cf341340"

                // V134.1 installs only for the current Windows user. This avoids
                // requiring a machine-wide/admin install for the preview build.
                perUserInstall = true
                dirChooser = true
                menuGroup = "Backlot"
            }
        }
    }
}
