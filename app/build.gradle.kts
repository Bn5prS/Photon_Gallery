plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.inferno.gallery"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.inferno.gallery"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    // Force Kotlin codegen — avoids 'unexpected jvm signature V' KSP bug with suspend Unit DAOs
    arg("room.generateKotlin", "true")
}

dependencies {
    // ── Compose BOM (aligns UI / Foundation / Runtime versions) ──
    implementation(platform(libs.androidx.compose.bom))

    // ── AndroidX Core ──
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ── Lifecycle ──
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // ── DataStore ──
    implementation(libs.androidx.datastore.preferences)

    // ── Compose UI ──
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // ── Material 3 Expressive (explicit version ≥ 1.4.0, overrides BOM) ──
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // ── Compose Animation (SharedTransitionScope, spring physics) ──
    implementation(libs.androidx.compose.animation)

    // ── Navigation ──
    implementation(libs.androidx.navigation.compose)

    // ── Graphics Shapes (RoundedPolygon, Morph — for shape morphing) ──
    implementation(libs.androidx.graphics.shapes)

    // ── Coil 3 — Image Loading ──
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.video)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)

    // ── EXIF ──
    implementation(libs.androidx.exifinterface)

    // ── Room — Database (stable 2.6.1, FTS5 support) ──
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

    // ── Paging 3 ──
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // ── ML Kit (OCR) ──
    implementation(libs.mlkit.text.recognition)

    // ── HuggingFace Tokenizer (DJL) ──
    // Replaced by custom BPETokenizer since DJL tokenizers fail to load JNI on Android

    // ── WorkManager ──
    implementation(libs.androidx.work.runtime.ktx)

    // ── Testing ──
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // ── Media3 (ExoPlayer) ──
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
}