plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.framebynavin.app"
    compileSdk = 35

    signingConfigs {
        create("dev") {
            storeFile = rootProject.file("keystore/REMOVED_PRIVATE_SIGNING_VALUE.jks")
            storePassword = "REMOVED_PRIVATE_SIGNING_VALUE"
            keyAlias = "REMOVED_PRIVATE_SIGNING_VALUE"
            keyPassword = "REMOVED_PRIVATE_SIGNING_VALUE"
        }
    }

    defaultConfig {
        applicationId = "com.framebynavin.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 35
        versionName = "1.7.3-24h-pulse-opportunities-rc1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("dev")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("com.google.android.gms:play-services-auth:21.6.0")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation(platform("androidx.compose:compose-bom:2025.04.01"))
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// v1.7.3 RC1: 24H Pulse + creator opportunities on top of the approved Insights 2.0 baseline.
// Persists lightweight YouTube counter snapshots, calculates rolling view/subscriber movement and top movers,
// and matches active performance opportunities back to useful saved Idea Vault entries.
