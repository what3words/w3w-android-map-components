// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath(libs.secrets.gradle.plugin)
        classpath(libs.ktlint.gradle)
        classpath(libs.sonarqube.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.gradle.ktlint) apply false
    alias(libs.plugins.autonomousapps.analysis)
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.layout.buildDirectory)
 }