plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
// The Play upload key is injected locally/through protected CI secrets.
// Never commit a key, its password, or a keystore to this repository.
val uploadEnv = listOf(
    "QREX_UPLOAD_STORE_FILE", "QREX_UPLOAD_STORE_PASSWORD",
    "QREX_UPLOAD_KEY_ALIAS", "QREX_UPLOAD_KEY_PASSWORD"
)
val suppliedUploadKeys = uploadEnv.count { !System.getenv(it).isNullOrBlank() }
if (suppliedUploadKeys in 1..3) {
    throw GradleException("Incomplete QREX upload signing configuration")
}
val uploadSigningReady = suppliedUploadKeys == uploadEnv.size

android {
    namespace = "com.brendigo.qrex"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.brendigo.qrex"
        minSdk = 26
        targetSdk = 36
        versionCode = 100
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        if (uploadSigningReady) {
            create("upload") {
                storeFile = file(System.getenv("QREX_UPLOAD_STORE_FILE"))
                storePassword = System.getenv("QREX_UPLOAD_STORE_PASSWORD")
                keyAlias = System.getenv("QREX_UPLOAD_KEY_ALIAS")
                keyPassword = System.getenv("QREX_UPLOAD_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            if (uploadSigningReady) signingConfig = signingConfigs.getByName("upload")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures { compose = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.08.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.zxing:core:3.5.3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
