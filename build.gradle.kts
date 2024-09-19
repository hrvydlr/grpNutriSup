plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

buildscript {
    val compose_version by extra("1.5.1")
    val kotlin_version by extra("1.9.10")

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    }
}
