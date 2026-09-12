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
        versionCode = 121
        versionName = "2.0.0-rc1-preflight"
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

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
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
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")

    implementation(platform("com.google.firebase:firebase-bom:34.18.0"))
    implementation("com.google.firebase:firebase-ai")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation(platform("androidx.compose:compose-bom:2025.04.01"))
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
