plugins {
    id("vetro.android.library")
    id("vetro.compose")
    id("vetro.kotlin.serialization")
}

android {
    namespace = "com.example.myapplication.designsystem"
}

dependencies {
    // Haze library for glassmorphism
    implementation("dev.chrisbanes.haze:haze:0.7.3")
}
