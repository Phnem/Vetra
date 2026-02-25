plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.apollo)
}

sqldelight {
    databases {
        create("AnimeDatabase") {
            packageName.set("com.example.myapplication.data.local")
        }
    }
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "v3.0.4-Stable"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GITHUB_OWNER", "\"Phnem\"")
        buildConfigField("String", "GITHUB_REPO", "\"Vetra\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-Xcontext-receivers"
        )
    }
}

dependencies {
    // 1. Compose: строго через BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.animation)
    debugImplementation(libs.compose.ui.tooling)

    // 2. Koin 4: строго через BOM
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // 3. Coil 3
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // 4. Навигация, UI, Стейт
    implementation(libs.navigation.compose)
    implementation(libs.activity.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.haze)
    implementation(libs.haze.materials)

    // 5. AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // 6. Dropbox
    implementation(libs.dropbox.core)
    implementation(libs.dropbox.android)

    // 7. OkHttp, WorkManager
    implementation(libs.okhttp)
    implementation(libs.work.runtime.ktx)

    // 8. SQLDelight
    implementation(libs.sqldelight.android.driver)
    implementation(libs.sqldelight.coroutines)

    // 9. Ktor (сеть)
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    // 10. Kotlin Serialization + Immutable collections (Zero Jank)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)

    // 11. Apollo (core:network; по умолчанию Apollo 4 использует OkHttp на Android)
    implementation(project(":core:network"))
    implementation(libs.apollo.runtime)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
