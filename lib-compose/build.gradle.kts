import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.maven.publish)
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("jacoco")
}

/**
 * IS_SNAPSHOT_RELEASE property will be automatically added to the root gradle.properties file by the CI pipeline, depending on the GitHub branch.
 * A snapshot release is generated for every pull request merged or commit made into an epic branch.
 */
val isSnapshotRelease = findProperty("IS_SNAPSHOT_RELEASE") == "true"
version =
    if (isSnapshotRelease) "${findProperty("LIBRARY_COMPOSE_VERSION")}-SNAPSHOT" else "${
        findProperty(
            "LIBRARY_COMPOSE_VERSION"
        )
    }"

android {
    namespace = "com.what3words.map.components.compose"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        buildConfigField(
            "String",
            "LIBRARY_VERSION",
            "\"${findProperty("LIBRARY_COMPOSE_VERSION")}\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            enableUnitTestCoverage = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget =
            org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(libs.versions.jvmToolchain.get())
    }
}

jacoco {
    toolVersion = libs.versions.jacoco.core.get()
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.withType<Test>().named("testDebugUnitTest"))

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/buttons/**",
        "**/providers/**",
        "**/utils/**",
        "**/models/**",
        "**/MapProvider*.*",
        "**/state/camera/**",
        "**/W3WMapComponent*.*",
        "**/W3WMapDefaults*.*",
    )

    val javaTree = fileTree("${buildDir}/intermediates/javac/debug") {
        exclude(fileFilter)
    }
    val kotlinTree = fileTree("${buildDir}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    sourceDirectories.setFrom(
        files(
            "${project.projectDir}/src/main/java",
            "${project.projectDir}/src/main/kotlin"
        )
    )
    classDirectories.setFrom(files(javaTree, kotlinTree))
    executionData.setFrom(fileTree("${buildDir}/outputs/unit_test_code_coverage/debugUnitTest").apply {
        include("testDebugUnitTest.exec")
    })
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

dependencies {
    // Material
    implementation(libs.android.material)
    implementation(libs.accompanist.permissions)

    // what3words
    api(libs.w3w.android.wrapper)
    api(libs.w3w.android.design.library)

    // Google maps
    compileOnly(libs.gms.maps)
    compileOnly(libs.googleMaps.utils)
    testImplementation(libs.gms.maps)
    implementation(libs.googleMaps.compose)

    // Mapbox
    implementation(libs.mapbox.ndk27)
    implementation(libs.mapbox.mapsCompose)

    // kotlin
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)
    api(libs.kotlinx.collections.immutable)
    api(libs.kotlinx.serialization.json)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.compose.material.icons)
    implementation(libs.androidx.compose.ui.viewbinding)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.arch.core.testing)
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

    coordinates("com.what3words", "w3w-android-map-components-compose", project.version.toString())

    pom {
        name.set("w3w-android-map-components-compose")
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