import java.net.URI
import java.util.Base64

plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.gradle.ktlint.get().pluginId)
    id(libs.plugins.maven.publish.get().pluginId)
    id(libs.plugins.signing.get().pluginId)
    alias(libs.plugins.dokka)
    id(libs.plugins.jreleaser.get().pluginId)
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
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    buildFeatures {
        viewBinding = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.material)
    // what3words
    api(libs.what3words.api.wrapper)

    // Google maps
    compileOnly(libs.googlemap.playservice)
    compileOnly(libs.googlemap.utils)
    testImplementation(libs.googlemap.playservice)

    // Mapbox
    compileOnly(libs.mapbox.v10)

    // kotlin
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)

    // Testing
    testImplementation(libs.junit4)
    testImplementation(libs.androidx.core)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.core.testing)
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
publishing {
    publications {
        create<MavenPublication>("maven") {
            afterEvaluate {
                from(components["release"])
            }

            groupId = "com.what3words"
            artifactId = "w3w-android-map-components"
            version = project.version.toString()

            withType(MavenPublication::class.java) {
                val publicationName = name
                val dokkaJar =
                    project.tasks.register("${publicationName}DokkaJar", Jar::class) {
                        group = JavaBasePlugin.DOCUMENTATION_GROUP
                        description = "Assembles Kotlin docs with Dokka into a Javadoc jar"
                        archiveClassifier.set("javadoc")
                        from(tasks.named("dokkaHtml"))

                        // Each archive name should be distinct, to avoid implicit dependency issues.
                        // We use the same format as the sources Jar tasks.
                        // https://youtrack.jetbrains.com/issue/KT-46466
                        archiveBaseName.set("${archiveBaseName.get()}-$publicationName")
                    }
                artifact(dokkaJar)
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
            // POM metadata
        }
    }

    repositories {
        mavenLocal()
        maven {
            name = "sonatypeSnapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            credentials {
                username = findProperty("MAVEN_CENTRAL_USERNAME") as? String
                password = findProperty("MAVEN_CENTRAL_PASSWORD") as? String
            }
        }
        maven {
            name = "stagingLocal"
            url = uri(layout.buildDirectory.dir("staging-deploy").get().asFile.absolutePath)
        }
    }
}

jreleaser {
    release {
        github {
            repoOwner = "what3words"
            overwrite = true
        }
    }

    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored.set(true)
        publicKey.set(
            findProperty("W3W_GPG_PUBLIC_KEY")?.toString()
                ?.let { String(Base64.getDecoder().decode(it)) } ?: "")
        secretKey.set(
            findProperty("W3W_GPG_SECRET_KEY")?.toString()
                ?.let { String(Base64.getDecoder().decode(it)) } ?: "")
        passphrase.set(findProperty("W3W_GPG_PASSPHRASE")?.toString())
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active.set(org.jreleaser.model.Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository(layout.buildDirectory.dir("staging-deploy").get().asFile.absolutePath)
                    username.set(findProperty("MAVEN_CENTRAL_USERNAME")?.toString())
                    password.set(findProperty("MAVEN_CENTRAL_PASSWORD")?.toString())
                    verifyPom.set(false)
                    setStage(org.jreleaser.model.api.deploy.maven.MavenCentralMavenDeployer.Stage.UPLOAD.toString())
                }
            }
        }
    }
}
//endregion