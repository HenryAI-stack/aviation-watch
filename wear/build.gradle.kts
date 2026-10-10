plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Enginetime cloud sync: OAuth client ("TVs and Limited Input devices") in the same
// Google Cloud project as the Enginetime web app. Never committed: supplied by
// ~/.gradle/gradle.properties (enginetime.clientId / enginetime.clientSecret) or by the
// ENGINETIME_CLIENT_ID / ENGINETIME_CLIENT_SECRET environment variables (GitHub secrets in CI).
// Without them the app still records engine times and keeps them on the watch.
fun enginetimeSetting(property: String, env: String): String =
    providers.gradleProperty(property).orElse(providers.environmentVariable(env)).getOrElse("")

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

        buildConfigField(
            "String",
            "ENGINETIME_CLIENT_ID",
            "\"${enginetimeSetting("enginetime.clientId", "ENGINETIME_CLIENT_ID")}\"",
        )
        buildConfigField(
            "String",
            "ENGINETIME_CLIENT_SECRET",
            "\"${enginetimeSetting("enginetime.clientSecret", "ENGINETIME_CLIENT_SECRET")}\"",
        )
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
        buildConfig = true
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
    implementation(libs.androidx.work.runtime.ktx)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
