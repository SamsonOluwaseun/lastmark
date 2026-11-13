plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.samlytics.lastmark"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.samlytics.lastmark"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.2.1"
    }

    // Releases are signed with one permanent key so updates install over the top.
    // CI writes the key from GitHub secrets. Without it, the debug key is used.
    val keystorePath = System.getenv("LASTMARK_KEYSTORE_PATH")
    val hasReleaseKey = !keystorePath.isNullOrBlank() && file(keystorePath).exists()

    signingConfigs {
        if (hasReleaseKey) {
            create("lastmark") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("LASTMARK_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("LASTMARK_KEY_ALIAS")
                keyPassword = System.getenv("LASTMARK_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (hasReleaseKey) signingConfigs.getByName("lastmark") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
