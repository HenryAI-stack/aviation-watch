plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.henryai.aviationwatch"
    // Current AndroidX releases require compiling against API 37. targetSdk
    // (runtime behaviour) stays at 36, the level of Wear OS 6 on the Watch4.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.henryai.aviationwatch"
        // Wear OS 3 (API 30) is the oldest version the Galaxy Watch4 shipped with.
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            // Enable R8 once the feature set stabilises; release builds are
            // signed locally with your own key (see docs/SETUP.md).
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // Kotlin's jvmTarget follows targetCompatibility with AGP built-in Kotlin.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(project(":core:aviation"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)
    implementation(libs.androidx.wear.input)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
