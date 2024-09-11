// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false

}

buildscript {
    // Instead of ext, we define versions directly as variables in Kotlin DSL
    val compose_version by extra("1.5.1") // Or the latest version you need
    val kotlin_version by extra("1.9.10") // Or the latest version you need

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    }
}
