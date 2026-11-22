plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
if (file("google-services.json").exists()) apply(plugin = "com.google.gms.google-services")

android {
    namespace = "com.ecoceipt.app"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.ecoceipt.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GEMINI_MODEL", "\"${providers.gradleProperty("geminiModel").getOrElse("gemini-2.5-flash-lite")}\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.12.1")
    implementation("androidx.compose.foundation:foundation:1.12.1")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.12.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.12.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-ai")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}

val verifyFigmaAssets by tasks.registering {
    val assetFolder = layout.projectDirectory.dir("src/main/assets/figma")
    doLast {
        val required = listOf("8d105.png", "53454.png", "7d5c6.png", "c7474.svg", "60c30.svg", "d2a24.svg", "eb40c.svg")
        val missing = required.filter { !assetFolder.file(it).asFile.isFile || assetFolder.file(it).asFile.length() == 0L }
        check(missing.isEmpty()) {
            "Aset Figma belum tersedia: ${missing.joinToString()}. Jalankan scripts/download-figma-assets.ps1 dari root proyek."
        }
    }
}
tasks.named("preBuild").configure { dependsOn(verifyFigmaAssets) }
