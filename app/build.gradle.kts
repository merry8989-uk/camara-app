plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Signing material is NEVER committed to this repository.
// CI decodes it from repository secrets into a file and points CAMARA_KEYSTORE at it.
// Locally, either set the same environment variables or drop your own keystore at
// keystore/camara-dev.p12 (that path is git-ignored).
val camaraKeystoreFile: java.io.File? = run {
    val env = System.getenv("CAMARA_KEYSTORE")
    // Resolve relative paths against the ROOT project, not this module — otherwise
    // "keystore/x.p12" would look in app/keystore/ and silently find nothing, and the
    // release build would quietly produce an unsigned APK.
    val candidate = if (!env.isNullOrBlank()) rootProject.file(env) else rootProject.file("keystore/camara-dev.p12")
    candidate.takeIf { it.exists() }
}

if (camaraKeystoreFile == null) {
    logger.lifecycle("[camara] No signing keystore found — release builds will be UNSIGNED.")
} else {
    logger.lifecycle("[camara] Signing with ${camaraKeystoreFile.absolutePath}")
}

// App Factory Rule 1: never ship a release without the real signing key.
//
// Checked against the task graph rather than at configuration time, so a fresh clone can
// still build debug without the keystore — but any release task without a key fails hard
// instead of quietly producing an unsigned APK.
gradle.taskGraph.whenReady {
    val wantsRelease = allTasks.any { it.name.contains("Release", ignoreCase = true) }
    if (wantsRelease && camaraKeystoreFile == null) {
        throw GradleException(
            "Refusing to build a release: no signing keystore found. Set CAMARA_KEYSTORE " +
                "(CI decodes it from the CAMARA_KEYSTORE_BASE64 secret), or place the key at " +
                "keystore/camara-dev.p12. An unsigned release is not allowed."
        )
    }
}

android {
    namespace = "com.camara.app"
    // Media3 1.10+ requires compiling against API 36.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.camara.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 14
        versionName = "1.5.4"

        vectorDrawables { useSupportLibrary = true }

        ndk {
            // Ship the two ABIs real phones actually use. The x86/x86_64 emulator
            // libraries roughly double the APK (ML models are ~20 MB per ABI) for no
            // benefit on hardware.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    signingConfigs {
        create("camara") {
            val ks = camaraKeystoreFile
            if (ks != null) {
                storeFile = ks
                storeType = "PKCS12"
                storePassword = System.getenv("CAMARA_STORE_PASSWORD").orEmpty()
                keyAlias = System.getenv("CAMARA_KEY_ALIAS") ?: "camara"
                keyPassword = System.getenv("CAMARA_KEY_PASSWORD").orEmpty()
                // v1 signing is enabled so the signing certificate stays inspectable
                // (META-INF/CERT.RSA) and CI can verify it.
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        // Minified on purpose: R8 cuts the (unminified) dex from ~68 MB to a few MB,
        // which is the single biggest lever on the download size.
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            if (camaraKeystoreFile != null) signingConfig = signingConfigs.getByName("camara")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (camaraKeystoreFile != null) signingConfig = signingConfigs.getByName("camara")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        // Needed for BuildConfig.VERSION_NAME, which the in-app updater compares against
        // the latest release tag (App Factory Rule 6).
        buildConfig = true
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    val camerax = "1.3.4"
    val media3 = "1.11.1"

    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)

    // Core + lifecycle + activity
    implementation("androidx.core:core-ktx:1.13.1")
    // Settings persistence (App Factory Rule 4: every setting persists and applies now)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.1")

    // CameraX
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")
    implementation("androidx.camera:camera-video:$camerax")
    implementation("androidx.camera:camera-extensions:$camerax")

    // Media3 — video playback, editing and export
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-transformer:$media3")
    implementation("androidx.media3:media3-effect:$media3")
    implementation("androidx.media3:media3-common:$media3")
    implementation("androidx.media3:media3-ui:$media3")

    // ML Kit, unbundled ("thin") variants: the models live in Google Play Services
    // instead of inside the APK. Same API, but face detection drops from ~6.9 MB to
    // ~0.8 MB of app size and subject segmentation from tens of MB of native code to
    // ~200 KB. The model downloads on first use (or at install time, see the manifest).
    implementation("com.google.android.gms:play-services-mlkit-face-detection:17.1.0")
    implementation("com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1")
    // Explicit, so we can report *why* a face feature is unavailable (unbundled models
    // can only be fetched by Play Services).
    implementation("com.google.android.gms:play-services-base:18.5.0")

    // EXIF orientation handling
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
