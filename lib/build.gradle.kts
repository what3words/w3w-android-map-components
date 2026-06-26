import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.gradle.ktlint.get().pluginId)
    alias(libs.plugins.dokka)
    alias(libs.plugins.vanniktech.maven.publish)
}

/**
 * IS_SNAPSHOT_RELEASE property will be automatically added to the root gradle.properties file by the CI pipeline, depending on the GitHub branch.
 * A snapshot release is generated for every pull request merged or commit made into an epic branch.
 */
val isSnapshotRelease = findProperty("IS_SNAPSHOT_RELEASE") == "true"
version =
    if (isSnapshotRelease) "${findProperty("LIBRARY_VERSION")}-SNAPSHOT" else "${findProperty("LIBRARY_VERSION")}"

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = "com.what3words.map.components"

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        named("debug") {
            enableUnitTestCoverage = true
        }
        named("release") {
            isMinifyEnabled = false
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    buildFeatures {
        viewBinding = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget =
            org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(libs.versions.jvmToolchain.get())
    }
}

dependencies {
    implementation(libs.android.material)
    // what3words
    api(libs.w3w.android.wrapper)

    // Google maps
    compileOnly(libs.gms.maps)
    compileOnly(libs.googleMaps.utils)
    testImplementation(libs.gms.maps)

    // Mapbox
    compileOnly(libs.mapbox.v10)

    // kotlin
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.arch.core.testing)
}

tasks.register("checkSnapshotDependencies") {
    doLast {
        val snapshotDependencies = allprojects.flatMap { project ->
            project.configurations
                .asSequence()
                .filter { it.isCanBeResolved }
                .flatMap { it.allDependencies }
                .filter { it.version?.contains("SNAPSHOT", ignoreCase = true) == true }
                .map { "${project.name}:${it.group}:${it.name}:${it.version}" }
                .distinct()
                .toList()
        }

        if (snapshotDependencies.isNotEmpty()) {
            snapshotDependencies.forEach { println("SNAPSHOT dependency found: $it") }
            throw GradleException("SNAPSHOT dependencies found.")
        } else {
            println("No SNAPSHOT dependencies found.")
        }
    }
}

//region publishing
mavenPublishing {
    // Uploads to the Central Portal staging deployment (manual release on the portal),
    // matching the previous jReleaser UPLOAD stage. SNAPSHOT versions are routed to the
    // Central snapshots repository automatically.
    publishToMavenCentral()
    signAllPublications()

    configure(
        AndroidSingleVariantLibrary(
            variant = "release",
            sourcesJar = SourcesJar.Sources(),
            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
        )
    )

    coordinates("com.what3words", "w3w-android-map-components", project.version.toString())

    pom {
        name.set("w3w-android-map-components")
        description.set("Android library to integrate what3words with different map providers.")
        url.set("https://github.com/what3words/w3w-android-map-components")
        licenses {
            license {
                name.set("The MIT License (MIT)")
                url.set("https://github.com/what3words/w3w-android-map-components/blob/master/LICENSE")
            }
        }
        developers {
            developer {
                id.set("what3words")
                name.set("what3words")
                email.set("development@what3words.com")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/what3words/w3w-android-map-components.git")
            developerConnection.set("scm:git:ssh://git@github.com:what3words/w3w-android-map-components.git")
            url.set("https://github.com/what3words/w3w-android-map-components")
        }
    }
}
//endregion