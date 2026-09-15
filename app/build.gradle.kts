plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

val releaseStoreFilePath = providers.gradleProperty("FRAMEBYNAVIN_RELEASE_STORE_FILE")
    .orElse(providers.environmentVariable("FRAMEBYNAVIN_RELEASE_STORE_FILE"))
    .orNull
    ?.takeIf { it.isNotBlank() }
val releaseStorePassword = providers.gradleProperty("FRAMEBYNAVIN_RELEASE_STORE_PASSWORD")
    .orElse(providers.environmentVariable("FRAMEBYNAVIN_RELEASE_STORE_PASSWORD"))
    .orNull
    ?.takeIf { it.isNotBlank() }
val releaseKeyAlias = providers.gradleProperty("FRAMEBYNAVIN_RELEASE_KEY_ALIAS")
    .orElse(providers.environmentVariable("FRAMEBYNAVIN_RELEASE_KEY_ALIAS"))
    .orNull
    ?.takeIf { it.isNotBlank() }
val releaseKeyPassword = providers.gradleProperty("FRAMEBYNAVIN_RELEASE_KEY_PASSWORD")
    .orElse(providers.environmentVariable("FRAMEBYNAVIN_RELEASE_KEY_PASSWORD"))
    .orNull
    ?.takeIf { it.isNotBlank() }
val productionSigningConfigured = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.framebynavin.app"
    compileSdk = 36

    signingConfigs {
        create("prototypeStable") {
            storeFile = rootProject.file("dev-signing/framebynavin-public-debug.jks")
            storePassword = "framebynavin-dev"
            keyAlias = "framebynavin-dev"
            keyPassword = "framebynavin-dev"
        }
        if (productionSigningConfigured) {
            create("productionRelease") {
                storeFile = rootProject.file(requireNotNull(releaseStoreFilePath))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    defaultConfig {
        applicationId = "com.framebynavin.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 130
        versionName = "2.0.0-rc6-cine-pulse-mascot-rebuild"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    val productionReleaseSigning = signingConfigs.findByName("productionRelease")
    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("prototypeStable")
        }
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            productionReleaseSigning?.let { signingConfig = it }
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

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.06.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.10.2")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.firebase:firebase-auth-ktx:23.2.1")
    implementation("com.google.firebase:firebase-appcheck-playintegrity:18.0.0")
    implementation("com.google.android.gms:play-services-drive:17.0.0")
    implementation("com.google.api-client:google-api-client-android:2.7.2")
    implementation("com.google.apis:google-api-services-youtube:v3-rev20250521-2.0.0")
    implementation("com.google.http-client:google-http-client-gson:1.47.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.06.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
