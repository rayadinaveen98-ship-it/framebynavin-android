import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.4.20"
    id("org.jetbrains.compose") version "1.12.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
}

group = "com.backlot"
version = "0.2.0"

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
            packageVersion = "0.2.0"
            description = "Backlot creator control room"
            vendor = "Backlot"

            windows {
                // Stable across Backlot desktop upgrades.
                upgradeUuid = "641dd83e-96cf-4e82-8c0b-70e8cf341340"
                perUserInstall = true
                dirChooser = true
                menuGroup = "Backlot"
            }
        }
    }
}
