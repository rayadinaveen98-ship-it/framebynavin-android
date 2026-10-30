import java.security.KeyStore
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey

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
        // RC12: RC6 UI preserved; explicit Android14+ full-screen access fallback.
        versionCode = 269
        versionName = "2.15.6-rc13-data-privacy-preview"
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
        // Personal sideload only: same signing certificate as previous Backlot
        // previews, but a non-debuggable APK. This key is PUBLIC and is not
        // a suitable production/Play signing identity.
        create("personalStable") {
            initWith(getByName("release"))
            isDebuggable = false
            signingConfig = signingConfigs.getByName("prototypeStable")
            matchingFallbacks += listOf("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}


// RC16: a release AAB or APK must NEVER silently use the public preview signer.
// This check does not affect debug or personalStable sideload APKs. A private
// upload key must be provisioned out-of-repository by the release owner.
val verifyPrivateReleaseSigning = tasks.register("verifyPrivateReleaseSigning") {
    group = "verification"
    description = "Reject missing, publicly shared, or invalid Backlot Play release signing keys"
    doLast {
        require(productionSigningConfigured) {
            "Private Play upload signing is not configured. Refusing a release build."
        }
        val file = rootProject.file(requireNotNull(releaseStoreFilePath)).canonicalFile
        val repositoryRoot = rootProject.projectDir.canonicalFile.toPath()
        require(file.isFile && !file.toPath().startsWith(repositoryRoot)) {
            "The private release keystore must exist outside the checked-out repository."
        }

        val releaseStore = try {
            KeyStore.getInstance(
                file, requireNotNull(releaseStorePassword).toCharArray()
            )
        } catch (_: Exception) {
            error("Private release keystore could not be read. Check the release credentials.")
        }
        val alias = requireNotNull(releaseKeyAlias)
        require(releaseStore.isKeyEntry(alias)) {
            "Private release key alias is missing or does not contain a signing key."
        }
        val releaseCertificate = requireNotNull(releaseStore.getCertificate(alias)) {
            "Private release signing certificate is unavailable."
        }
        val releasePublicKey = releaseCertificate.publicKey
        require(releasePublicKey is RSAPublicKey &&
            releasePublicKey.modulus.bitLength() >= 2048) {
            "Play upload certificate must use RSA with at least 2048 bits."
        }
        // Load the known checked-in *public* development certificate, not a
        // hard-coded fingerprint that could become stale if the fixture changes.
        val previewKeystore = rootProject.file("dev-signing/framebynavin-public-debug.jks")
        val previewStore = KeyStore.getInstance(
            previewKeystore, "framebynavin-dev".toCharArray()
        )
        val previewCertificate = requireNotNull(
            previewStore.getCertificate("framebynavin-dev")
        ) { "The public prototype signing certificate cannot be inspected." }
        require(!releaseCertificate.encoded.contentEquals(previewCertificate.encoded)) {
            "Private Play upload certificate is identical to the PUBLIC prototype signer."
        }

        // Signing has not yet happened here; only confirm key use is possible,
        // without printing passwords, filesystem secrets or private key data.
        try {
            val key = releaseStore.getKey(alias, requireNotNull(releaseKeyPassword).toCharArray())
            require(key is PrivateKey) {
                "Private release alias is not a signing private key."
            }
        } catch (_: Exception) {
            error("Private release key could not be unlocked with the configured password.")
        }
        println("PASS: external private RSA upload signer differs from public Backlot preview signer.")
    }
}
// Forbid producing either a release APK or an upload bundle without the gate.
// No dependency on preview/personalStable variants: existing owner updates work.
tasks.matching {
    it.name in setOf("assembleRelease", "bundleRelease", "packageReleaseBundle", "signReleaseBundle")
}.configureEach {
    dependsOn(verifyPrivateReleaseSigning)
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    // Explicit lifecycle owner for Spark voice cleanup with wrapped Compose contexts.
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.android.gms:play-services-auth:21.5.0")
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-ai")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-appcheck-debug")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
