plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.what3words.map.components.compose"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // what3words
    api(libs.what3words.api.wrapper)
    api(libs.what3words.designLibrary)

    // Google maps
    compileOnly(libs.googlemap.playservice)
    compileOnly(libs.googlemap.utils)
    testImplementation(libs.googlemap.playservice)
    implementation(libs.googlemap.compose)

    // Mapbox
    implementation(libs.mapbox.v11)
    implementation(libs.extension.mapbox.compose)

    // kotlin
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.material3)
    implementation(libs.compose.ui)
    debugImplementation(libs.compose.ui.tooling)

    // Testing
    testImplementation(libs.junit4)
    testImplementation(libs.androidx.core)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.core.testing)
}