// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath(libs.secrets.gradle.plugin)
        classpath(libs.ktlint.gradle)
        classpath(libs.sonarqube.gradle.plugin)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.jacoco.core)
        // Other plugins on this classpath pull in jakarta.activation, but AGP 9's
        // bundled JAXB impl still needs the legacy javax.activation API to parse
        // the SDK metadata. Provide it explicitly to avoid NoClassDefFoundError.
        classpath("javax.activation:activation:1.1.1")
    }
}

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.gradle.ktlint) apply false
    alias(libs.plugins.jreleaser) apply false
    alias(libs.plugins.autonomousapps.dependency.analysis)
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.layout.buildDirectory)
 }