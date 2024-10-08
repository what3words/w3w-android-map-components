// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath("com.google.android.libraries.mapsplatform.secrets-gradle-plugin:secrets-gradle-plugin:2.0.1")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:10.1.0")
        classpath("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:3.3")
    }
}

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "10.1.0" apply false
    id("com.android.library") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "1.8.20" apply false
    id("com.autonomousapps.dependency-analysis") version "1.20.0"
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.layout.buildDirectory)
 }