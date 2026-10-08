// Top-level build file. Plugins are declared here (not applied) so every
// module resolves the same versions from gradle/libs.versions.toml.
// AGP 9 compiles Kotlin itself (built-in Kotlin); declaring the Kotlin plugins
// here also lifts the Kotlin compiler AGP uses to the catalog's version.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
